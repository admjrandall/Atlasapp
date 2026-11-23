package com.jmr.atlas

import androidx.test.core.app.ApplicationProvider
import com.jmr.atlas.data.Repository
import com.jmr.atlas.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataTest {
    private lateinit var repo: Repository
    @Before fun setUp() { repo = Repository(ApplicationProvider.getApplicationContext(), null) }

    @Test fun oneDocumentInThreeProjectsIsOneRow() {
        val doc = repo.create(KObject(type = ObjType.DOCUMENT, title = "Insurance policy"))
        val ps = (1..3).map { repo.create(KObject(type = ObjType.PROJECT, title = "P$it", projectStatus = ProjectStatus.ACTIVE)) }
        ps.forEach { assertTrue(repo.link(doc, it, RelType.BELONGS_TO)) }
        assertEquals(1, repo.snap.live.count { it.title == "Insurance policy" })
        assertEquals(3, repo.snap.relationsOf(doc).size)
    }

    @Test fun duplicateEdgesAreRefused() {
        val a = repo.create(KObject(type = ObjType.PERSON, title = "Ana")); val b = repo.create(KObject(type = ObjType.PROJECT, title = "Garden"))
        assertTrue(repo.link(a, b)); assertFalse(repo.link(a, b)); assertFalse(repo.link(b, a)); assertFalse(repo.link(a, a))
        assertEquals(1, repo.snap.relations.size)
    }

    @Test fun editingAnObjectKeepsItsConnections() {
        // regression guard: INSERT OR REPLACE would delete the row and cascade away relationships and activity
        val a = repo.create(KObject(type = ObjType.PERSON, title = "Ana")); val b = repo.create(KObject(type = ObjType.PROJECT, title = "Garden"))
        repo.link(a, b)
        repo.update(repo.snap.objects[a]!!.copy(title = "Ana Ruiz"))
        assertEquals(1, repo.snap.relationsOf(a).size)
        assertTrue(repo.snap.activityOf(a).size >= 2)
    }

    @Test fun purgeCascadesTrashHides() {
        val a = repo.create(KObject(type = ObjType.PERSON, title = "Ana")); val b = repo.create(KObject(type = ObjType.PROJECT, title = "Garden"))
        repo.link(a, b)
        repo.trash(a)
        assertTrue(repo.snap.relationsOf(b).isEmpty())          // hidden while trashed
        assertTrue(repo.search("Ana").exact.isEmpty())
        repo.restore(a)
        assertEquals(1, repo.snap.relationsOf(b).size)
        repo.purge(a)
        assertNull(repo.snap.objects[a]); assertTrue(repo.snap.relations.isEmpty()); assertTrue(repo.snap.activity.none { it.objectId == a })
    }

    @Test fun fileOriginalIsEncryptedAndRecoverable() {
        val bytes = "Policy number AB-12 renews March 3".toByteArray()
        val cap = repo.captureFile(bytes, "text/plain", "policy.txt")
        val doc = repo.snap.objects[repo.snap.captures.first { it.id == cap }.attachmentId]!!
        val raw = java.io.File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "vault/${doc.vaultFile}").readBytes()
        assertFalse(String(raw, Charsets.ISO_8859_1).contains("Policy number"))
        assertArrayEquals(bytes, repo.files.read(doc.vaultFile))
        repo.purge(doc.id)
        assertNull(repo.files.read(doc.vaultFile))
    }

    @Test fun searchFindsExactAndConnected() {
        val p = repo.create(KObject(type = ObjType.PROJECT, title = "Home Renovation", projectStatus = ProjectStatus.ACTIVE))
        val j = repo.create(KObject(type = ObjType.PERSON, title = "Jamie Carter", role = "Contractor"))
        val n = repo.create(KObject(type = ObjType.NOTE, title = "Site visit", body = "Wall between kitchen and living room is not load-bearing"))
        repo.link(j, p); repo.link(n, p)
        val r = repo.search("load-bearing")
        assertEquals(listOf(n), r.exact.map { it.id })
        assertTrue(r.connected.any { it.first.id == p })
        assertEquals(j, repo.search("contractor").exact.first().id)
    }

    @Test fun networkIsHttpsOnlyAndNeverPrivate() {
        // 0.5 uses the internet only for pages you save and the model download — HTTPS only, never your local network.
        assertThrows(IllegalArgumentException::class.java) { com.jmr.atlas.integrations.Web.fetch("https://192.168.1.1/admin", true) }
        assertThrows(IllegalArgumentException::class.java) { com.jmr.atlas.integrations.Web.fetch("https://127.0.0.1/", true) }
        assertFalse(com.jmr.atlas.integrations.Web.isPublic("10.0.0.5")); assertFalse(com.jmr.atlas.integrations.Web.isPublic("100.101.102.103")); assertFalse(com.jmr.atlas.integrations.Web.isPublic("169.254.1.1"))
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val perms = ctx.packageManager.getPackageInfo(ctx.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toList()
        assertFalse("no SMS / call log / location access", perms.any { it.contains("SMS") || it.contains("CALL_LOG") || it.contains("LOCATION") })
    }

    @Test fun exportContainsEverythingButFileBytes() {
        val j = repo.create(KObject(type = ObjType.PERSON, title = "Jamie"))
        val json = org.json.JSONObject(repo.exportJson())
        assertEquals("atlas-export", json.getString("format"))
        assertEquals(j, json.getJSONArray("objects").getJSONObject(0).getString("id"))
    }
}
