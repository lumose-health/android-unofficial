# Medical Disclaimer

This is the disclaimer of record for `android-unofficial`: the GlycemicGPT Android phone app, the Wear OS watch face and companion, and the Bluetooth device data driver plugins (Tandem, Medtronic). The [platform repository's disclaimer](https://github.com/lumose-health/GlycemicGPT/blob/main/MEDICAL-DISCLAIMER.md) is the disclaimer of record for the backend API, web dashboard, and AI sidecar; where the two conflict on a point they both address, the platform disclaimer wins.

## Regulatory Status

This software has **not** been cleared, approved, or certified by any regulatory authority worldwide, including but not limited to:

- The U.S. Food and Drug Administration (FDA)
- EU Notified Bodies (no CE marking under MDR 2017/745)
- Health Canada
- Australia's Therapeutic Goods Administration (TGA)
- Any equivalent national regulatory authority

## Not a Medical Device

**This software is NOT a medical device.** It is experimental open-source software provided for educational and informational purposes only. No individual, organization, or entity associated with this project is the "manufacturer" of a medical device under any regulatory framework.

GlycemicGPT does not control any medical devices. It reads data from insulin pumps and continuous glucose monitors (CGMs), displays that data, and provides AI-generated text suggestions. These suggestions are not medical advice and must not be treated as such.

## No Therapeutic Write Surface

This repository is the part of GlycemicGPT that talks to insulin pumps directly, so its monitoring-only posture is stated here rather than by reference:

- The app and its shipped drivers **read** glucose, insulin-on-board, basal, bolus history, and pump hardware status. They do not issue therapeutic writes -- no bolus dosing, no basal rate changes, no pump-setting modifications.
- The plugin SDK in this repository has **no insulin delivery primitives**: there is no API on any capability interface for issuing a bolus, modifying basal rates, or otherwise writing therapeutic state to a pump.
- Non-therapeutic device-management operations that do exist in the SDK -- CGM calibration, BLE pair/unpair, connect/disconnect -- are session and lifecycle operations, not therapy, and remain permitted.

Any change that introduced a therapeutic write surface would move this software into a regulatory category it is not built, tested, or licensed for. The technical detail of this boundary, and the contribution rules that hold it, are in [CONTRIBUTING.md](CONTRIBUTING.md#device-data-drivers).

## Health Data Processing

This software processes health data including:

- Continuous glucose monitor (CGM) readings
- Insulin pump telemetry (basal rates, bolus history, insulin on board)
- Pump hardware status (battery, reservoir levels)
- User-configured therapy parameters (target glucose ranges, insulin ratios)
- Meal entries and, if you use the food-photo feature, the photos you submit for carb estimation

Data read from your pump is stored on your device and, if you configure a backend, sent to the self-hosted GlycemicGPT instance you control. See [PRIVACY.md](PRIVACY.md) for what this app stores and where it sends it.

AI analysis is performed by the backend you point this app at, not on the phone. Which AI provider that backend uses -- and therefore where your health data goes for inference -- is a decision made in your backend configuration, not by this app. The consequences of that choice, including the difference between cloud-hosted and local AI providers, are set out in the [platform repository's disclaimer](https://github.com/lumose-health/GlycemicGPT/blob/main/MEDICAL-DISCLAIMER.md#health-data-processing). It is the user's responsibility to review the data-handling policy of any provider that will receive their health data before configuring it.

## AI Limitations

AI-generated suggestions surfaced in this app -- the daily brief, chat responses, and any analysis relayed from your backend -- are produced by large language models (LLMs) that are known to:

- **Hallucinate** -- generate plausible-sounding but incorrect information
- **Misinterpret data** -- draw incorrect conclusions from glucose readings
- **Provide outdated information** -- not reflect the latest medical guidelines
- **Lack context** -- not understand your complete medical history, comorbidities, or current medications

All AI-generated content in this software is labeled as suggestions, not medical advice. Never act on AI suggestions without consulting your healthcare team.

## Critical Warnings

1. **Do not replace professional medical care.** Always consult with your endocrinologist, diabetes educator, or healthcare provider before making any changes to your diabetes management.

2. **Verify all suggestions.** Any insulin dosing, carb ratio, or correction factor suggestions must be verified with your healthcare team before use.

3. **Use extreme caution.** Incorrect diabetes management can result in severe hypoglycemia, diabetic ketoacidosis (DKA), or other life-threatening conditions.

4. **Alerts here are not a substitute for your pump's and CGM's own alerts.** Keep the alerts on your pump and CGM enabled. Alerts raised by this app depend on a Bluetooth connection holding, Android scheduling background work, your watch being in range, and your phone being charged. They are a supplement to your device's own alerts, never a replacement.

5. **If you experience a diabetes emergency, contact your healthcare provider or emergency services immediately.** Do not rely on this software for emergency medical guidance.

## Project-Owned Unofficial Builds and Third-Party Forks

This repository ships only monitoring and analysis builds. The plugin SDK is read-only by design.

This repository is the home of the **project-owned unofficial builds** -- the sideloaded Android and Wear OS apps distributed through this repository's Releases page, outside the Google Play Store. They include the read-only plugin SDK so users can extend the platform with additional **device data drivers**. The project does not ship, document, or solicit any plugin that controls insulin delivery or modifies pump settings; the SDK has no insulin delivery primitives (no bolus dosing, no basal rate changes, no therapeutic write surface). The same monitoring-only stance applies to these builds as to the platform's official builds (the Docker images and web app in the platform repository).

Because these builds are sideloaded, install APKs only from this repository's official [Releases](../../releases) page. An APK obtained anywhere else is not covered by anything stated in this document.

**Third-party forks are a separate matter.** Forks of this project that modify the SDK to add device control, insulin delivery, or any other pump-write functionality operate **outside the GlycemicGPT project**. The maintainers do not review them, recommend them, accept liability for them, or accept contributions to this repository whose intent is to enable them.

Users who choose to build, install, or run a third-party fork that introduces device control become the **manufacturer of their personal medical device** and accept full responsibility for that decision. This follows the same legal posture used by DIY diabetes projects such as Loop and AndroidAPS -- independent, community-built systems whose users have long operated as the manufacturers of their own personal medical devices.

## Untested Device Compatibility

This software may declare protocol compatibility with devices that have **not been tested against physical hardware**. Protocol compatibility (shared BLE protocol, authentication mechanism, and data formats) does not guarantee correct operation. Specifically:

- Data displayed from untested devices may be inaccurate, delayed, or missing
- BLE connection behavior, reconnection stability, and pairing flows may differ
- Safety-critical values (insulin on board, glucose readings, basal rates) must always be verified against the device manufacturer's official companion app
- Users who choose to use this software with untested device hardware accept all associated risk

The per-device verification status is published in the [README](README.md#overview) and is the authoritative statement of which pumps have been exercised against physical hardware and which have not. Check it before trusting a reading.

No contributor, maintainer, or entity associated with this project is liable for any adverse outcome resulting from use with untested or partially-tested device hardware. If in doubt, use only the device manufacturer's official software.

## LIMITATION OF LIABILITY

THE AUTHORS, CONTRIBUTORS, AND MAINTAINERS OF THIS SOFTWARE PROVIDE IT "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT.

IN NO EVENT SHALL THE AUTHORS, CONTRIBUTORS, OR MAINTAINERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM, OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

THIS INCLUDES BUT IS NOT LIMITED TO ANY DAMAGES, INJURIES, OR ADVERSE HEALTH OUTCOMES RESULTING FROM THE USE OF THIS SOFTWARE. BY USING GLYCEMICGPT, YOU ACKNOWLEDGE THAT:

- You are using this software entirely at your own risk
- You will not rely solely on AI-generated suggestions for medical decisions
- You understand that AI can and will make errors
- You will maintain regular care with qualified healthcare professionals
- You accept full responsibility for any decisions made based on this software's output
- No individual or entity associated with this project is liable for medical outcomes

## License Warranty Disclaimer

This software is licensed under the GNU General Public License v3.0 (GPL-3.0). Per Sections 15-17 of the GPL-3.0:

- **Section 15:** THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.
- **Section 16:** IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES.
- **Section 17:** If the disclaimer of warranty and limitation of liability provided above cannot be given local legal effect according to their terms, reviewing courts shall apply local law that most closely approximates an absolute waiver of all civil liability in connection with the Program.

See the [LICENSE](LICENSE) file for the complete GPL-3.0 text.

**Jurisdictional note:** Limitation of liability clauses for personal injury may be unenforceable in some jurisdictions, including under EU consumer protection law, UK consumer rights legislation, and Australian consumer law. The primary risk mitigation strategy of this project is its monitoring-only design -- shipped builds do not provide device control or insulin delivery capability. Users who run forks of this project that add such capabilities operate under a build-from-source model where the individual user becomes the "manufacturer" of their personal build, consistent with the precedent set by Loop, AndroidAPS, and other DIY diabetes projects. This disclaimer does not constitute legal advice.

---

*Last reviewed: 2026-07-23.*
