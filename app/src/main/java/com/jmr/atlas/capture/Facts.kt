package com.jmr.atlas.capture

import com.jmr.atlas.model.*
import java.time.LocalDate
import java.time.Month

/**
 * Key facts read from a document's text on this phone (no AI): total, validity/due dates, reference numbers, the
 * document's own date and its kind. Each fact is labelled as read from the document so you can correct it.
 */
object Facts {
    data class Result(val facts: List<Detail>, val docDate: LocalDate?, val kind: String)

    private val money = Regex("(?:US)?\\$\\s?([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.([0-9]{2}))?")
    private val totalRx = Regex("(?i)\\b(total|amount due|balance due|grand total|total due)\\b[^\\n$]{0,24}\\$\\s?([0-9][0-9,]*(?:\\.[0-9]{2})?)")
    private val dateRx = Regex("(?i)\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\.?\\s+(\\d{1,2}),?\\s+(\\d{4})\\b|\\b(\\d{1,2})/(\\d{1,2})/(\\d{2,4})\\b|\\b(\\d{4})-(\\d{2})-(\\d{2})\\b")
    private val validRx = Regex("(?i)\\b(valid until|expires?(?: on)?|due(?: date)?|pay by|renews?(?: on)?|expiration(?: date)?)[:\\s]+")
    private val refRx = Regex("(?i)\\b(invoice|estimate|quote|policy|account|reference|ref|order|claim|confirmation)\\s*(?:no\\.?|number|#|num)?\\s*[:#]?\\s*((?=[A-Z0-9-]*\\d)[A-Z0-9][A-Z0-9-]{4,})\\b")
    private val KINDS = listOf("Invoice", "Estimate", "Quote", "Receipt", "Contract", "Policy", "Statement", "Permit", "Warranty", "Manual", "Letter", "Report", "Proposal")

    fun parseDate(m: MatchResult): LocalDate? = runCatching {
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> LocalDate.of(g[3].toInt(), Month.entries.first { it.name.startsWith(g[1].take(3).uppercase()) }, g[2].toInt())
            g[4].isNotEmpty() -> { val y = g[6].toInt().let { if (it < 100) 2000 + it else it }; LocalDate.of(y, g[4].toInt(), g[5].toInt()) }
            else -> LocalDate.of(g[7].toInt(), g[8].toInt(), g[9].toInt())
        }
    }.getOrNull()

    fun extract(text: String, objectId: String = ""): Result {
        if (text.isBlank()) return Result(emptyList(), null, "")
        val out = mutableListOf<Detail>()
        fun fact(label: String, value: String, type: FieldType = FieldType.TEXT, secret: Boolean = false) {
            if (out.none { it.label == label }) out += Detail(objectId = objectId, kind = DetailKind.FACT, label = label, value = value, type = type, flag = secret, extra = "Read from the document", origin = Origin.EXTRACTED)
        }
        val head = text.take(4000)
        totalRx.find(text)?.let { fact(it.groupValues[1].replaceFirstChar { c -> c.uppercase() }, "$" + it.groupValues[2], FieldType.MONEY) }
            ?: money.findAll(text).maxByOrNull { it.groupValues[1].replace(",", "").toLongOrNull() ?: 0 }?.takeIf { (it.groupValues[1].replace(",", "").toLongOrNull() ?: 0) >= 10 }
                ?.let { fact("Largest amount", "$" + it.groupValues[1] + (it.groupValues[2].takeIf { c -> c.isNotEmpty() }?.let { c -> ".$c" } ?: ""), FieldType.MONEY) }
        validRx.findAll(text).forEach { m ->
            val after = text.substring(m.range.last + 1).take(30)
            dateRx.find(after)?.let { parseDate(it) }?.let { d -> fact(m.groupValues[1].replaceFirstChar { it.uppercase() }, d.toString(), FieldType.DATE) }
        }
        refRx.find(head)?.let { fact("${it.groupValues[1].replaceFirstChar { c -> c.uppercase() }} number", it.groupValues[2], secret = it.groupValues[1].lowercase() in setOf("policy", "account")) }
        val docDate = dateRx.find(head)?.let { parseDate(it) }?.takeIf { it.isAfter(LocalDate.of(1990, 1, 1)) && it.isBefore(LocalDate.now().plusYears(2)) }
        val kind = KINDS.firstOrNull { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(head.take(600)) }.orEmpty()
        return Result(out.take(6), docDate, kind)
    }
}
