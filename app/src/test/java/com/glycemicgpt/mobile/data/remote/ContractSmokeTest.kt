package com.glycemicgpt.mobile.data.remote

import com.glycemicgpt.mobile.contract.ContractFixtures
import com.glycemicgpt.mobile.data.remote.dto.LoginRequest
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import java.lang.reflect.Method

/**
 * Cross-repo contract smoke test (GLY-92, AC2 + AC5).
 *
 * Now that the Android app and the backend ship on independent cadences, this
 * guards the HTTP contract from the client side. It exercises the **real**
 * Retrofit interface ([GlycemicGptApi]), the **real** app Moshi configuration,
 * and the **real** response DTOs against the vendored, pinned OpenAPI spec.
 *
 * Three invariants:
 *  1. **Golden round-trip** -- a representative, **hand-authored** payload
 *     (written to match the pinned spec, not generated from it) deserializes and
 *     populates the fields the app reads. This gates DTO regressions on the client
 *     side; it does not by itself detect backend drift -- the spec-coupled
 *     endpoint/method and field-presence checks below do that.
 *  2. **Additive drift is tolerated** -- an unknown/extra field (a backend that
 *     added a field, or an older client reading a newer server) deserializes
 *     fine. This is the client half of the backend tolerant-reader posture
 *     (see docs/adr/0002-backend-tolerant-reader-compat.md).
 *  3. **Incompatible drift fails** -- a field the app reads is renamed/removed,
 *     or its type changes, and parsing throws.
 *
 * Plus an endpoint-presence check: every endpoint the app calls must still exist
 * in the pinned spec, so a dropped backend route is caught when the pin is
 * refreshed.
 *
 * This is a smoke test, not a full DTO<->spec structural diff -- see
 * docs/adr/0003-contract-drift-strategy.md for why that tradeoff was chosen.
 */
class ContractSmokeTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GlycemicGptApi

    // Mirror NetworkModule.provideMoshi() exactly so the test uses the app's real
    // deserialization configuration, not a bespoke one.
    private val moshi: Moshi = Moshi.Builder().add(InstantAdapter()).build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GlycemicGptApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueue(body: String) {
        server.enqueue(MockResponse().setBody(body))
    }

    /** Assert the enqueued body fails to deserialize with a Moshi data exception. */
    private fun assertIncompatible(block: suspend () -> Unit) {
        val thrown =
            try {
                runBlocking { block() }
                null
            } catch (t: Throwable) {
                t
            }
        assertNotNull("Expected incompatible payload to fail parsing, but it parsed", thrown)
        val chain = generateSequence(thrown) { it.cause }
        assertTrue(
            "Expected a JsonDataException in the failure chain, got $thrown",
            chain.any { it is JsonDataException },
        )
    }

    // --- 1. Golden round-trips (safety-relevant + core surfaces) ---

    @Test
    fun `health response round-trips`() = runBlocking {
        enqueue("""{"status":"ok"}""")
        assertEquals("ok", api.healthCheck().body()!!.status)
    }

    @Test
    fun `login response round-trips including nested user`() = runBlocking {
        enqueue(
            """
            {"access_token":"a","refresh_token":"r","token_type":"bearer",
             "expires_in":3600,"user":{"id":"u1","email":"e@x.com","role":"patient"}}
            """.trimIndent(),
        )
        val body = api.login(LoginRequest("e@x.com", "pw")).body()!!
        assertEquals("a", body.accessToken)
        assertEquals("r", body.refreshToken)
        assertEquals(3600, body.expiresIn)
        assertEquals("u1", body.user.id)
    }

    @Test
    fun `alert thresholds round-trip -- the on-device floor fires from these`() = runBlocking {
        enqueue(
            """{"urgent_low":55.0,"low_warning":70.0,"high_warning":180.0,"urgent_high":250.0}""",
        )
        val body = api.getAlertThresholds().body()!!
        assertEquals(55.0f, body.urgentLow, 0.0f)
        assertEquals(250.0f, body.urgentHigh, 0.0f)
        // iob_warning is optional; absent here parses to null (tolerated).
        assertNull(body.iobWarning)
    }

    @Test
    fun `safety limits round-trip -- glucose validity bounds`() = runBlocking {
        enqueue(
            """
            {"min_glucose_mgdl":20,"max_glucose_mgdl":500,"max_basal_rate_milliunits":3000,
             "max_bolus_dose_milliunits":25000,"updated_at":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        val body = api.getSafetyLimits().body()!!
        assertEquals(20, body.minGlucoseMgDl)
        assertEquals(500, body.maxGlucoseMgDl)
    }

    // --- 2. Additive drift is tolerated ---

    @Test
    fun `unknown extra fields are ignored (additive drift tolerated)`() = runBlocking {
        // A newer backend adds fields the app doesn't know about.
        enqueue(
            """
            {"urgent_low":55.0,"low_warning":70.0,"high_warning":180.0,"urgent_high":250.0,
             "iob_warning":4.5,"future_field":"ignored","nested_future":{"a":1}}
            """.trimIndent(),
        )
        val body = api.getAlertThresholds().body()!!
        assertEquals(55.0f, body.urgentLow, 0.0f)
        assertEquals(4.5f, body.iobWarning!!, 0.0f)
    }

    @Test
    fun `optional field absent is tolerated (older backend)`() = runBlocking {
        // An older backend that predates glucose_unit_source still parses.
        enqueue("""{"glucose_unit":"mmol"}""")
        val body = api.getGlucoseUnit().body()!!
        assertEquals("mmol", body.glucoseUnit)
        assertNull(body.glucoseUnitSource)
    }

    // --- 3. Incompatible drift fails ---

    @Test
    fun `renamed required field fails (incompatible drift)`() {
        // access_token -> accessToken: the field the app reads is gone.
        enqueue(
            """
            {"accessToken":"a","refresh_token":"r","token_type":"bearer",
             "expires_in":3600,"user":{"id":"u1","email":"e@x.com","role":"patient"}}
            """.trimIndent(),
        )
        assertIncompatible { api.login(LoginRequest("e@x.com", "pw")) }
    }

    @Test
    fun `missing required nested field fails (incompatible drift)`() {
        // user object lost its required id.
        enqueue(
            """
            {"access_token":"a","refresh_token":"r","token_type":"bearer",
             "expires_in":3600,"user":{"email":"e@x.com","role":"patient"}}
            """.trimIndent(),
        )
        assertIncompatible { api.login(LoginRequest("e@x.com", "pw")) }
    }

    @Test
    fun `wrong field type fails (incompatible drift)`() {
        // min_glucose_mgdl is an Int in the contract; a string is incompatible.
        enqueue(
            """
            {"min_glucose_mgdl":"twenty","max_glucose_mgdl":500,"max_basal_rate_milliunits":3000,
             "max_bolus_dose_milliunits":25000,"updated_at":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertIncompatible { api.getSafetyLimits() }
    }

    // --- Endpoint-presence coupling to the pinned spec ---

    @Test
    fun `every endpoint the app calls exists in the pinned spec`() {
        val spec = ContractFixtures.pinnedSpec().getJSONObject("paths")
        // Map normalized spec path -> set of HTTP methods declared for it.
        val specOps: Map<String, Set<String>> =
            spec.keys().asSequence().associate { rawPath ->
                normalizePath(rawPath) to
                    spec.getJSONObject(rawPath).keys().asSequence().map { it.lowercase() }.toSet()
            }

        val missing =
            GlycemicGptApi::class.java.declaredMethods
                .mapNotNull(::retrofitOp)
                .distinct()
                .filter { (verb, path) ->
                    val methods = specOps[normalizePath(path)]
                    methods == null || verb.lowercase() !in methods
                }

        assertTrue(
            "These endpoints the app calls are absent from (or use a different HTTP " +
                "method than) the pinned contract (${ContractFixtures.PINNED_SPEC_PATH}); " +
                "refresh the pin and reconcile with the backend before shipping: $missing",
            missing.isEmpty(),
        )
    }

    // --- Field-level coupling to the pinned spec (safety-critical responses) ---

    @Test
    fun `pinned spec still declares the safety-critical fields the app reads`() {
        // Unlike the golden round-trips (hand-written payloads), this couples
        // directly to the pinned contract: a refreshed pin that drops or renames a
        // consumed field on one of these responses fails here.
        assertConsumedFieldsPresent(
            "/api/settings/safety-limits",
            "get",
            listOf("min_glucose_mgdl", "max_glucose_mgdl"),
        )
        assertConsumedFieldsPresent(
            "/api/settings/alert-thresholds",
            "get",
            listOf("urgent_low", "low_warning", "high_warning", "urgent_high"),
        )
        assertConsumedFieldsPresent(
            "/api/auth/mobile/login",
            "post",
            listOf("access_token", "refresh_token", "expires_in", "user"),
        )
        // The Nightscout data response is consumed into Room; the mapper drops any
        // pump event whose nullable `units` is null, so a silent rename of `units`
        // would drop all NS bolus/basal and understate IOB. Couple it explicitly,
        // along with the glucose-reading value the app reads.
        assertArrayItemFieldsPresent(
            "/api/integrations/nightscout/{connection_id}/data",
            "get",
            "pump_events",
            listOf("units", "event_type"),
        )
        assertArrayItemFieldsPresent(
            "/api/integrations/nightscout/{connection_id}/data",
            "get",
            "glucose_readings",
            listOf("value"),
        )
    }

    @Test
    fun `vendored CONTRACT_VERSION matches the pinned spec x-contract-version`() {
        val fileVersion = ContractFixtures.readRepoFile("contract/CONTRACT_VERSION").trim()
        val specVersion =
            ContractFixtures.pinnedSpec().getJSONObject("info").getString("x-contract-version")
        assertEquals(
            "contract/CONTRACT_VERSION ($fileVersion) disagrees with openapi.json " +
                "info.x-contract-version ($specVersion); refresh the pin and its version together.",
            specVersion,
            fileVersion,
        )
    }

    private fun assertConsumedFieldsPresent(
        path: String,
        method: String,
        fields: List<String>,
    ) {
        val props = responseSchemaProperties(path, method)
        val missing = fields.filter { it !in props }
        assertTrue(
            "Pinned spec response for $method $path no longer declares consumed field(s) " +
                "$missing (present: $props). A refreshed pin dropped/renamed a field the app " +
                "reads; reconcile the DTOs with the backend before shipping.",
            missing.isEmpty(),
        )
    }

    /** Assert every field is declared in the item schema of an array-typed response property. */
    private fun assertArrayItemFieldsPresent(
        path: String,
        method: String,
        arrayProperty: String,
        fields: List<String>,
    ) {
        val props = arrayItemSchemaProperties(path, method, arrayProperty)
        val missing = fields.filter { it !in props }
        assertTrue(
            "Pinned spec response for $method $path array `$arrayProperty` no longer declares " +
                "item field(s) $missing (present: $props). A refreshed pin dropped/renamed a " +
                "field the app reads; reconcile the DTOs with the backend before shipping.",
            missing.isEmpty(),
        )
    }

    /** Resolve an endpoint's 200-response schema properties from the pinned spec. */
    private fun responseSchemaProperties(path: String, method: String): Set<String> =
        schemaProperties(responseSchema(path, method))

    /** Resolve the item schema properties of an array-typed property on the 200 response. */
    private fun arrayItemSchemaProperties(
        path: String,
        method: String,
        arrayProperty: String,
    ): Set<String> {
        val itemSchema =
            responseSchema(path, method).getJSONObject("properties")
                .getJSONObject(arrayProperty).getJSONObject("items")
        return schemaProperties(itemSchema)
    }

    /** The resolved 200-response schema object (following its top-level `$ref`). */
    private fun responseSchema(path: String, method: String): org.json.JSONObject {
        val spec = ContractFixtures.pinnedSpec()
        val schema =
            spec.getJSONObject("paths").getJSONObject(path).getJSONObject(method)
                .getJSONObject("responses").getJSONObject("200")
                .getJSONObject("content").getJSONObject("application/json")
                .getJSONObject("schema")
        return resolveRef(schema)
    }

    /** Follow a `$ref` (if present) to its components schema; otherwise return as-is. */
    private fun resolveRef(schema: org.json.JSONObject): org.json.JSONObject {
        if (!schema.has("\$ref")) return schema
        val name = schema.getString("\$ref").substringAfterLast('/')
        return ContractFixtures.pinnedSpec().getJSONObject("components")
            .getJSONObject("schemas").getJSONObject(name)
    }

    private fun schemaProperties(schema: org.json.JSONObject): Set<String> {
        val resolved = resolveRef(schema)
        val props = resolved.getJSONObject("properties")
        return props.keys().asSequence().toSet()
    }

    /** Extract the (HTTP method, path) from a Retrofit method's verb annotation, if any. */
    private fun retrofitOp(method: Method): Pair<String, String>? {
        method.getAnnotation(GET::class.java)?.let { return "get" to it.value }
        method.getAnnotation(POST::class.java)?.let { return "post" to it.value }
        method.getAnnotation(PUT::class.java)?.let { return "put" to it.value }
        method.getAnnotation(DELETE::class.java)?.let { return "delete" to it.value }
        method.getAnnotation(PATCH::class.java)?.let { return "patch" to it.value }
        return null
    }

    /** Normalize path templates so Retrofit `{deviceToken}` matches spec `{device_token}`. */
    private fun normalizePath(path: String): String = path.replace(Regex("\\{[^}]+}"), "{}")
}
