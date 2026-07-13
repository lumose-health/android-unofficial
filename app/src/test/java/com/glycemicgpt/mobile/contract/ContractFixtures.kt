package com.glycemicgpt.mobile.contract

import org.json.JSONObject
import java.io.File

/**
 * Shared helpers for the cross-repo contract guards (GLY-92 / 56.9).
 *
 * Both the contract smoke test and the safety-constant drift guard need to read
 * files from the repo root (the vendored OpenAPI pin, and production Kotlin
 * sources) rather than from the module's classpath. Unit tests run with the
 * working directory set to the module project dir, so we walk up the directory
 * tree -- the same working-directory-walk idiom
 * [com.glycemicgpt.mobile.wear.WatchFacePusherTest] uses to find the app module,
 * except here the target is the repo root (marked by `settings.gradle.kts`).
 */
object ContractFixtures {

    /** The vendored, pinned OpenAPI contract from the backend monorepo. */
    const val PINNED_SPEC_PATH = "contract/openapi.json"

    /** Walk up from the test working directory to the repo root (has settings.gradle.kts). */
    fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate repo root from ${System.getProperty("user.dir")}")
    }

    /** Read a repo-root-relative file, failing with a clear message if it moved. */
    fun readRepoFile(relativePath: String): String {
        val file = File(repoRoot(), relativePath)
        check(file.isFile) {
            "Expected file '$relativePath' not found at ${file.absolutePath}. " +
                "If it moved, update the guard and the constants source-of-truth doc " +
                "(docs/contract/safety-constants.md)."
        }
        return file.readText()
    }

    /** The parsed pinned OpenAPI spec. */
    fun pinnedSpec(): JSONObject = JSONObject(readRepoFile(PINNED_SPEC_PATH))
}
