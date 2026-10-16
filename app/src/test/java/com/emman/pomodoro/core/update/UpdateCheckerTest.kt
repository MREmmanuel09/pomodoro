package com.emman.pomodoro.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private val releaseJson = """
        {
          "tag_name": "v1.0.3",
          "html_url": "https://github.com/MREmmanuel09/pomodoro/releases/tag/v1.0.3",
          "body": "notas",
          "draft": false,
          "assets": [
            {
              "name": "app-release.apk",
              "browser_download_url": "https://github.com/MREmmanuel09/pomodoro/releases/download/v1.0.3/app-release.apk"
            },
            {
              "name": "checksums.txt",
              "browser_download_url": "https://example.com/checksums.txt"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parseLatestRelease_newerVersion_returnsInfoWithApk() {
        val info = UpdateChecker.parseLatestRelease(releaseJson, "1.0.2")

        assertEquals("1.0.3", info?.version)
        assertEquals(
            "https://github.com/MREmmanuel09/pomodoro/releases/download/v1.0.3/app-release.apk",
            info?.apkUrl
        )
        assertEquals(
            "https://github.com/MREmmanuel09/pomodoro/releases/tag/v1.0.3",
            info?.releaseUrl
        )
    }

    @Test
    fun parseLatestRelease_sameVersion_returnsNull() {
        assertNull(UpdateChecker.parseLatestRelease(releaseJson, "1.0.3"))
    }

    @Test
    fun parseLatestRelease_olderVersion_returnsNull() {
        assertNull(UpdateChecker.parseLatestRelease(releaseJson, "1.0.4"))
    }

    @Test
    fun parseLatestRelease_invalidJson_returnsNull() {
        assertNull(UpdateChecker.parseLatestRelease("no es json", "1.0.2"))
    }

    @Test
    fun parseLatestRelease_noApkAsset_returnsInfoWithNullApk() {
        val json = """
            {
              "tag_name": "1.1.0",
              "html_url": "https://example.com/release",
              "assets": [{"name": "notes.txt", "browser_download_url": "https://example.com/notes.txt"}]
            }
        """.trimIndent()

        val info = UpdateChecker.parseLatestRelease(json, "1.0.2")

        assertEquals("1.1.0", info?.version)
        assertNull(info?.apkUrl)
    }

    @Test
    fun parseLatestRelease_blankTag_returnsNull() {
        val json = """{"tag_name": "", "html_url": "https://example.com"}"""

        assertNull(UpdateChecker.parseLatestRelease(json, "1.0.2"))
    }

    @Test
    fun isNewer_patchBump_returnsTrue() {
        assertTrue(UpdateChecker.isNewer("1.0.3", "1.0.2"))
    }

    @Test
    fun isNewer_minorBump_returnsTrue() {
        assertTrue(UpdateChecker.isNewer("1.1", "1.0.5"))
    }

    @Test
    fun isNewer_numericNotLexicographic() {
        assertTrue(UpdateChecker.isNewer("1.0.10", "1.0.9"))
        assertFalse(UpdateChecker.isNewer("1.0.9", "1.0.10"))
    }

    @Test
    fun isNewer_equalVersions_returnsFalse() {
        assertFalse(UpdateChecker.isNewer("1.0.2", "1.0.2"))
    }

    @Test
    fun isNewer_olderVersion_returnsFalse() {
        assertFalse(UpdateChecker.isNewer("1.0.1", "1.0.2"))
    }

    @Test
    fun isNewer_suffixVersions_returnsFalse() {
        assertFalse(UpdateChecker.isNewer("1.0.3-beta", "1.0.2"))
        assertFalse(UpdateChecker.isNewer("1.0.3", "1.0.2-debug"))
    }
}
