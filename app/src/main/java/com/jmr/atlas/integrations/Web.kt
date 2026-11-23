package com.jmr.atlas.integrations

import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.time.LocalDate

/**
 * Link previews. Atlas downloads only the page you saved (and its preview image), over HTTPS, with hard size and time
 * limits. Pages are untrusted data: nothing in them is executed, and their text is stored as plain text.
 * Addresses on your local network, loopback or link-local are refused so a shared link can't probe your router.
 */
object Web {
    data class Page(
        val url: String, val title: String, val description: String, val site: String, val author: String, val published: LocalDate?,
        val text: String, val readMinutes: Int, val image: ByteArray?, val imageMime: String,
    )

    private const val MAX_HTML = 2 * 1024 * 1024
    private const val MAX_IMAGE = 3 * 1024 * 1024
    private const val UA = "Mozilla/5.0 (Linux; Android 17) AtlasLinkPreview/0.5"
    private val URL_RX = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)

    /** The single URL in a piece of text, if the text is essentially just a link. */
    fun soleUrl(text: String): String? {
        val t = text.trim()
        val m = URL_RX.findAll(t).toList()
        if (m.size != 1) return null
        val rest = t.replace(m[0].value, "").trim()
        return if (rest.length <= 120) m[0].value.trimEnd('.', ',', ')', ']') else null
    }
    fun firstUrl(text: String) = URL_RX.find(text)?.value?.trimEnd('.', ',', ')', ']')
    fun host(url: String) = runCatching { URI(url).host.removePrefix("www.") }.getOrDefault(url)

    fun isPublic(host: String): Boolean = runCatching {
        InetAddress.getAllByName(host).all { a ->
            !(a.isAnyLocalAddress || a.isLoopbackAddress || a.isLinkLocalAddress || a.isSiteLocalAddress || a.isMulticastAddress ||
                (a is Inet4Address && (a.address[0].toInt() and 0xff == 100 && a.address[1].toInt() and 0xc0 == 64)) ||   // 100.64/10 (CGNAT, Tailscale)
                (a is Inet6Address && (a.address[0].toInt() and 0xfe) == 0xfc))                                        // fc00::/7
        }
    }.getOrDefault(false)

    /** Fetches a page. [keepText] = also keep a readable copy. Throws on refusal or failure. */
    fun fetch(raw: String, keepText: Boolean): Page {
        var url = raw.trim().let { if (it.startsWith("http://", true)) "https://" + it.substring(7) else it }
        var conn: HttpURLConnection
        var hops = 0
        while (true) {
            val u = URL(url)
            require(u.protocol == "https") { "Only HTTPS pages are fetched" }
            require(isPublic(u.host)) { "That address is on a private network" }
            conn = (u.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false; connectTimeout = 8000; readTimeout = 10000
                setRequestProperty("User-Agent", UA); setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location") ?: error("Redirect without a location")
                conn.disconnect(); url = URL(u, loc).toString(); require(++hops <= 5) { "Too many redirects" }; continue
            }
            require(code in 200..299) { "The page answered $code" }
            break
        }
        val type = conn.contentType.orEmpty()
        require(type.contains("html", true)) { "Not a web page ($type)" }
        val html = conn.inputStream.use { readCapped(it, MAX_HTML) }.toString(charset(type))
        conn.disconnect()

        val meta = metaTags(html)
        fun m(vararg k: String) = k.firstNotNullOfOrNull { meta[it]?.takeIf { v -> v.isNotBlank() } }.orEmpty()
        val title = m("og:title", "twitter:title").ifBlank { Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)?.groupValues?.get(1)?.let(::decode)?.trim().orEmpty() }
        val text = readable(html)
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        val imgUrl = m("og:image", "og:image:url", "twitter:image").takeIf { it.isNotBlank() }?.let { runCatching { URL(URL(url), it).toString() }.getOrNull() }
        val img = imgUrl?.let { runCatching { image(it) }.getOrNull() }
        return Page(
            url = url, title = title.take(200), description = m("og:description", "description", "twitter:description").take(600),
            site = m("og:site_name").ifBlank { host(url) }, author = m("author", "article:author", "twitter:creator").take(80).takeIf { !it.startsWith("http") }.orEmpty(),
            published = m("article:published_time", "date", "pubdate").take(10).let { runCatching { LocalDate.parse(it) }.getOrNull() },
            text = if (keepText) text.take(100_000) else "", readMinutes = if (words > 150) (words / 230).coerceAtLeast(1) else 0,
            image = img?.first, imageMime = img?.second.orEmpty(),
        )
    }

    private fun image(src: String): Pair<ByteArray, String>? {
        val u = URL(src)
        if (u.protocol != "https" || !isPublic(u.host)) return null
        val c = (u.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 10000; setRequestProperty("User-Agent", UA) }
        try {
            if (c.responseCode !in 200..299) return null
            val t = c.contentType.orEmpty()
            if (!t.startsWith("image/") || t.contains("svg")) return null
            return c.inputStream.use { readCapped(it, MAX_IMAGE) }.toByteArray() to t.substringBefore(';')
        } finally { c.disconnect() }
    }

    private fun readCapped(s: java.io.InputStream, max: Int): java.io.ByteArrayOutputStream {
        val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(16 * 1024)
        while (true) { val n = s.read(buf); if (n < 0) break; out.write(buf, 0, n); require(out.size() <= max) { "Page is too large" } }
        return out
    }

    private fun charset(type: String) = runCatching { java.nio.charset.Charset.forName(Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(type)?.groupValues?.get(1) ?: "UTF-8") }.getOrDefault(Charsets.UTF_8)

    fun metaTags(html: String): Map<String, String> {
        val head = html.substringBefore("</head>", html.take(200_000))
        val out = LinkedHashMap<String, String>()
        Regex("<meta\\s+([^>]+)>", RegexOption.IGNORE_CASE).findAll(head).forEach { m ->
            val attrs = Regex("([a-zA-Z:-]+)\\s*=\\s*(\"[^\"]*\"|'[^']*')").findAll(m.groupValues[1]).associate { it.groupValues[1].lowercase() to decode(it.groupValues[2].trim('"', '\'')) }
            val key = (attrs["property"] ?: attrs["name"] ?: attrs["itemprop"])?.lowercase() ?: return@forEach
            attrs["content"]?.let { out.putIfAbsent(key, it.trim()) }
        }
        return out
    }

    /** Plain readable text: prefers <article>/<main>, drops scripts, styles, navigation and markup. */
    fun readable(html: String): String {
        val flags = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        var h = html.replace(Regex("<(script|style|noscript|svg|nav|header|footer|form|aside|iframe)[^>]*>.*?</\\1>", flags), " ")
        (Regex("<article[^>]*>(.*?)</article>", flags).find(h) ?: Regex("<main[^>]*>(.*?)</main>", flags).find(h))?.let { h = it.groupValues[1] }
        h = h.replace(Regex("<(br|/p|/div|/li|/h[1-6]|/tr)[^>]*>", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]+>"), " ")
        return decode(h).lines().map { it.replace(Regex("[ \\t\\u00a0]+"), " ").trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    fun decode(s: String): String = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
        .replace("&nbsp;", " ").replace("&mdash;", "—").replace("&ndash;", "–").replace("&hellip;", "…").replace("&rsquo;", "’").replace("&lsquo;", "‘").replace("&ldquo;", "“").replace("&rdquo;", "”")
        .replace(Regex("&#(\\d+);")) { runCatching { String(Character.toChars(it.groupValues[1].toInt())) }.getOrDefault("") }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { runCatching { String(Character.toChars(it.groupValues[1].toInt(16))) }.getOrDefault("") }
}
