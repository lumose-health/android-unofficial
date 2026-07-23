// Top-level build file for GlycemicGPT Android app
import app.cash.licensee.LicenseeExtension
import app.cash.licensee.UnusedAction

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.licensee) apply false
}

// Licence policy for everything the APKs redistribute.
//
// Declared once here rather than in each module so the three shipped APKs (:app,
// :wear-device, :watchface) cannot drift into disagreeing about what is acceptable to
// ship, and so a dependency added to any of them is judged against the same list.
// Licensee reads each module's resolved release runtime graph and fails the build on
// anything this block does not cover, which is what stops a Renovate bump from shipping
// an unattributed dependency: a new licence family is a build failure, not a silent
// omission from the attribution document.
//
// `allowDependency` entries are artifacts whose POM carries no SPDX identifier. Each one
// is a human judgement about a specific version, recorded with its reason; a version bump
// re-raises the judgement rather than inheriting it.
subprojects {
    plugins.withId("app.cash.licensee") {
        extensions.configure<LicenseeExtension> {
            // This is one policy shared by three modules with different dependency graphs, so
            // most entries are legitimately unused in any single module (a wear-only dependency
            // is "unused" when :app is checked). Ignore unused entries rather than emit a warning
            // per entry per module; a stale entry is a minor maintenance cost, not a build signal.
            unusedAction(UnusedAction.IGNORE)

            allow("Apache-2.0")
            allow("MIT")
            allow("BSD-2-Clause")
            allow("BSD-3-Clause")
            allow("ICU")
            allow("SAX-PD")
            allow("SAX-PD-2.0")

            // EPL-1.0 is deliberately NOT blanket-allowed: the FSF considers it incompatible
            // with GPLv3 for a combined, distributed work, and this application is GPL-3.0-only.
            // The single EPL-1.0 artifact below is a deep transitive of the alpha
            // androidx.wear.watchface validator, pulled into the wear APK. It is allowed by
            // coordinate, not by family, so a new EPL dependency fails the gate for a human to
            // weigh. The GPL/EPL compatibility of this transitive is flagged for legal review
            // before the first stable release; see the PR discussion.
            allowDependency(
                "com.rackspace.eclipse.webtools.sourceediting",
                "org.eclipse.wst.xml.xpath2.processor",
                "2.1.100",
            ) {
                because(
                    "EPL-1.0 XPath 2.0 processor, a transitive of the alpha wear watchface " +
                        "validator; GPL-3.0 compatibility flagged for legal review",
                )
            }

            // GPL-3.0 is this project's own licence; javasake is an ecosystem library
            // published under it. Compatible by definition with a GPL-3.0-only app.
            allowUrl("https://www.gnu.org/licenses/gpl-3.0.txt") {
                because("GPL-3.0-only, the same licence this application ships under")
            }
            // commonmark 0.13.0 predates SPDX identifiers in its POM; the URL is the
            // canonical BSD-2-Clause text and META-INF/LICENSE.txt in the artifact agrees.
            allowUrl("http://opensource.org/licenses/BSD-2-Clause") {
                because("BSD-2-Clause declared by URL rather than SPDX identifier")
            }

            allowDependency("com.google.android.gms", "play-services-base", "18.5.0") {
                because("Android Software Development Kit License (Google Play services)")
            }
            allowDependency("com.google.android.gms", "play-services-basement", "18.4.0") {
                because("Android Software Development Kit License (Google Play services)")
            }
            allowDependency("com.google.android.gms", "play-services-tasks", "18.2.0") {
                because("Android Software Development Kit License (Google Play services)")
            }
            allowDependency("com.google.android.gms", "play-services-wearable", "18.2.0") {
                because("Android Software Development Kit License (Google Play services)")
            }

            allowDependency("com.twelvemonkeys.common", "common-image", "3.9.4") {
                because("BSD-3-Clause, declared in the POM as the unmapped name 'The BSD License'")
            }
            allowDependency("com.twelvemonkeys.common", "common-io", "3.9.4") {
                because("BSD-3-Clause, declared in the POM as the unmapped name 'The BSD License'")
            }
            allowDependency("com.twelvemonkeys.common", "common-lang", "3.9.4") {
                because("BSD-3-Clause, declared in the POM as the unmapped name 'The BSD License'")
            }
            allowDependency("com.twelvemonkeys.imageio", "imageio-core", "3.9.4") {
                because("BSD-3-Clause, declared in the POM as the unmapped name 'The BSD License'")
            }
            allowDependency("com.twelvemonkeys.imageio", "imageio-metadata", "3.9.4") {
                because("BSD-3-Clause, declared in the POM as the unmapped name 'The BSD License'")
            }
            allowDependency("com.twelvemonkeys.imageio", "imageio-webp", "3.9.4") {
                because("BSD-3-Clause, declared in the POM as the unmapped name 'The BSD License'")
            }

            allowDependency("edu.princeton.cup", "java-cup", "10k") {
                because("CUP Parser Generator licence, a permissive MIT-style grant")
            }
            allowDependency("net.zetetic", "sqlcipher-android", "4.13.0") {
                because("SQLCipher BSD-style licence from Zetetic; see docs/THIRD_PARTY_LICENSES.md")
            }
            allowDependency("org.bouncycastle", "bcprov-jdk18on", "1.84") {
                because("Bouncy Castle Licence, an MIT-style permissive grant")
            }

            // JitPack builds publish no licence metadata in their generated POMs. Each of
            // these is Apache-2.0 at source; the identifier cannot be read from the POM.
            allowDependency("com.github.jeziellago", "Markwon", "58aa5aba6a") {
                because("Apache-2.0 upstream; JitPack POMs carry no licence metadata")
            }
            allowDependency("com.github.jeziellago", "compose-markdown", "0.5.8") {
                because("Apache-2.0 upstream; JitPack POMs carry no licence metadata")
            }
            allowDependency("com.github.xgouchet", "AXML", "v1.0.1") {
                because("Apache-2.0 upstream; JitPack POMs carry no licence metadata")
            }
        }
    }
}

// Dependency locking: every module commits a gradle.lockfile so OSV-Scanner has a Gradle
// manifest to CVE-scan -- it does not parse build.gradle.kts or libs.versions.toml (see
// docs/dev/security-testing.md). On Renovate PRs the regen-gradle-lockfiles.yml workflow
// refreshes the lockfiles (Renovate's own container lacks the Android SDK and cannot).
// After changing dependencies manually, regenerate with:
//   ./gradlew resolveAndLockAll --write-locks
allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    // Do not lock two narrow configuration families (everything else, including other
    // AGP-generated configurations such as _agp_internal_*, stays locked and scanned):
    //  - "_internal-unified-test-platform-*" is AGP's bundled instrumented-test host tooling
    //    (netty, protobuf, ...). Its versions are pinned by AGP itself, never ship in an APK,
    //    and can only move via an AGP bump -- locking them would put AGP's internal supply
    //    chain on our CVE gate with findings we cannot fix.
    //  - "*DependenciesMetadata" configurations are Kotlin compile-metadata views, not real
    //    classpaths; they bypass the conflict resolution the actual classpaths perform.
    fun excludedFromLocking(configurationName: String) =
        configurationName.startsWith("_internal-unified-test-platform") ||
            configurationName.endsWith("DependenciesMetadata")

    configurations.configureEach {
        if (excludedFromLocking(name)) {
            resolutionStrategy.deactivateDependencyLocking()
        }
    }

    tasks.register("resolveAndLockAll") {
        notCompatibleWithConfigurationCache("Filters configurations at execution time")
        doFirst {
            require(gradle.startParameter.isWriteDependencyLocks) {
                "$path must be run with the --write-locks flag"
            }
        }
        doLast {
            configurations.filter { it.isCanBeResolved }.forEach {
                try {
                    // Resolve the dependency graph only (not artifacts): lock state is
                    // written from the graph, and artifact-variant selection fails for a
                    // few AGP configurations outside their task context (e.g. the app's
                    // own androidTest compile classpath).
                    it.incoming.resolutionResult.root
                } catch (e: Exception) {
                    // The configurations excluded from locking above are AGP-internal and may
                    // not resolve outside their task context; that is expected. Any other
                    // resolution failure would mean an incomplete lockfile -- fail loudly.
                    if (excludedFromLocking(it.name)) {
                        logger.warn("Skipping unresolvable configuration ${it.name}: ${e.message}")
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
