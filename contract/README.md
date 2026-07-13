# Pinned API contract

`openapi.json` here is a **byte-for-byte vendored copy** of the backend platform
repo's `apps/api/contract/openapi.json` — the pinned, versioned snapshot of the
FastAPI HTTP schema. The app diffs its Retrofit/Moshi surface against this pin so
a separately-shipped backend cannot silently break the client
(GLY-92 / Epic 56.9).

| File | Purpose |
|---|---|
| `openapi.json` | The pinned backend contract. Consumed by `ContractSmokeTest`. |
| `CONTRACT_VERSION` | The contract version this pin was validated against (mirrors `info.x-contract-version` inside `openapi.json`). |

**Validated against `CONTRACT_VERSION` = 1.**

## Refreshing the pin

1. Copy `apps/api/contract/openapi.json` from the platform repo over
   `contract/openapi.json` here (byte-for-byte — do not hand-edit).
2. Set `contract/CONTRACT_VERSION` to the value in the new spec's
   `info.x-contract-version`.
3. Run `./gradlew :app:testDebugUnitTest`. If `ContractSmokeTest` fails, the change
   is **incompatible** on a covered surface (a consumed field or called endpoint
   changed/disappeared) — reconcile the DTOs / interface with the backend before
   shipping. A pass means the **covered** compatibility checks held (the exercised
   responses, the safety-critical field set, and endpoint/method presence); it does
   not prove every schema change is additive. Review schema changes to surfaces the
   smoke test does not exercise before relying on the pass.

See `docs/adr/0003-contract-drift-strategy.md` for the strategy and
`docs/contract/safety-constants.md` for the duplicated safety constants.
