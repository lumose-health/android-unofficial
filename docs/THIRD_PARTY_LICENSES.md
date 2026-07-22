# Third-Party Licenses

## Tandem BLE Protocol Implementation

The Bluetooth Low Energy protocol implementation in this application is informed
by research and reverse-engineering work from the following MIT-licensed projects:

### pumpX2

- Repository: https://github.com/jwoglom/pumpX2
- License: MIT
- Copyright: James Woglom

Java library implementing a reverse-engineered Bluetooth protocol for Tandem
insulin pumps. Our Kotlin implementation is based on studying this protocol
documentation and message format. No code is imported or used as a runtime
dependency.

### controlX2

- Repository: https://github.com/jwoglom/controlX2
- License: MIT
- Copyright: James Woglom

Android + Wear OS reference application for Tandem insulin pumps. Architecture
patterns for BLE service management and pump pairing were studied from this
project. No code is imported or used as a runtime dependency.

---

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

## Medtronic MiniMed BLE Protocol Implementation (OpenMinimed)

Unlike the Tandem implementation above (an independent reimplementation that
imports no upstream code), the Medtronic MiniMed 700-series read-only driver is
a **direct dependency / port** of the OpenMinimed project. OpenMinimed's SAKE
handshake is consumed from Maven Central (`org.openminimed:javasake`) as a
**runtime dependency** compiled into the app, and
its read-only readers are ported line-for-line into Kotlin — not merely studied
as a reference. This is possible because OpenMinimed is GPL-3.0 and GlycemicGPT
is itself GPL-3.0, so the licenses are compatible and copyleft propagation is a
non-issue.

The driver is used **with the explicit permission of the OpenMinimed author**
(palmarci / Pál Marci), who relicensed the work to **GPL-3.0** for this purpose.
Upstream copyright notices and GPL-3.0 headers are retained verbatim in the
published artifact and every ported file; in-source headers in
`plugins/shipped/medtronic/` cite the specific upstream source file each port is
derived from.

### OpenMinimed

- Organization: https://github.com/OpenMinimed
- License: GPL-3.0
- Copyright: palmarci (Pál Marci) and contributors — drfubar, Morten Fyhn
  Amundsen, Stenium. Original `medtronic-bt-decrypt` proof-of-concept by
  @planiitis.

The four repositories this driver is built from:

| Repository | Role in this app |
|---|---|
| [PythonSake](https://github.com/OpenMinimed/PythonSake) | Reference implementation of the 6-stage SAKE symmetric authenticated key exchange. |
| [PythonPumpConnector](https://github.com/OpenMinimed/PythonPumpConnector) | Authoritative read-only reader logic (Linux/Python). The CGM, IDD status, history, device-info, and battery readers are **ported to Kotlin** from this project. |
| [JavaSake](https://github.com/OpenMinimed/JavaSake) | Production-grade SAKE handshake for the JVM/Android. Consumed as the **Maven Central dependency `org.openminimed:javasake`** (package `org.openminimed.sake`) and driven through `MedtronicSakeSession`. |
| [JavaPumpConnector](https://github.com/OpenMinimed/JavaPumpConnector) | Android BLE peripheral scaffolding (permissions, advertising). Informed the connection-manager structure; no data readers exist upstream. |

The Android/JVM ports of JavaSake and JavaPumpConnector are maintained under
[jlengelbrecht](https://github.com/jlengelbrecht) (the GlycemicGPT project lead).

The firmware-derived SAKE key material that the handshake depends on is published
upstream by OpenMinimed under the same GPL-3.0 license (and ships in the JavaSake
artifact); it introduces no new secret.

---

GPL-3.0 License

The OpenMinimed-derived code above, like GlycemicGPT as a whole, is licensed
under the GNU General Public License, version 3. The full license text ships at
the repository root in [`LICENSE`](../LICENSE). In short, you may use, study,
share, and modify this software, provided that derivative works are distributed
under the same license and that copyright and license notices are preserved.

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

---

## EC-JPAKE Cryptographic Handshake (Tandem)

### Particle (ecjpake-java)

- Repository: https://github.com/particle-iot/ecjpake-java
- License: Apache-2.0
- Copyright: 2022 Particle Industries, Inc.

---

The terms and conditions below are the full text of the Apache License,
Version 2.0. The upstream repository's boilerplate "APPENDIX: How to apply
the Apache License to your work" section is instructional and not part of
the license terms, so it is omitted here.

Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction,
and distribution as defined by Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by
the copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all
other entities that control, are controlled by, or are under common
control with that entity. For the purposes of this definition,
"control" means (i) the power, direct or indirect, to cause the
direction or management of such entity, whether by contract or
otherwise, or (ii) ownership of fifty percent (50%) or more of the
outstanding shares, or (iii) beneficial ownership of such entity.

"You" (or "Your") shall mean an individual or Legal Entity
exercising permissions granted by this License.

"Source" form shall mean the preferred form for making modifications,
including but not limited to software source code, documentation
source, and configuration files.

"Object" form shall mean any form resulting from mechanical
transformation or translation of a Source form, including but
not limited to compiled object code, generated documentation,
and conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or
Object form, made available under the License, as indicated by a
copyright notice that is included in or attached to the work
(an example is provided in the Appendix below).

"Derivative Works" shall mean any work, whether in Source or Object
form, that is based on (or derived from) the Work and for which the
editorial revisions, annotations, elaborations, or other modifications
represent, as a whole, an original work of authorship. For the purposes
of this License, Derivative Works shall not include works that remain
separable from, or merely link (or bind by name) to the interfaces of,
the Work and Derivative Works thereof.

"Contribution" shall mean any work of authorship, including
the original version of the Work and any modifications or
additions to that Work or Derivative Works thereof, that is
intentionally submitted to Licensor for inclusion in the
Work by the copyright owner or by an individual or Legal
Entity authorized to submit on behalf of the copyright owner.
For the purposes of this definition, "submitted" means any form
of electronic, verbal, or written communication sent to the
Licensor or its representatives, including but not limited to
communication on electronic mailing lists, source code control
systems, and issue tracking systems that are managed by, or on
behalf of, the Licensor for the purpose of discussing and
improving the Work, but excluding communication that is
conspicuously marked or otherwise designated in writing by the
copyright owner as "Not a Contribution."

"Contributor" shall mean Licensor and any individual or Legal
Entity on behalf of whom a Contribution has been received by
Licensor and subsequently incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable
copyright license to reproduce, prepare Derivative Works of,
publicly display, publicly perform, sublicense, and distribute
the Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable
(except as stated in this section) patent license to make, have made,
use, offer to sell, sell, import, and otherwise transfer the Work,
where such license applies only to those patent claims licensable
by such Contributor that are necessarily infringed by their
Contribution(s) alone or by combination of their Contribution(s)
with the Work to which such Contribution(s) was submitted. If You
institute patent litigation against any entity (including a
cross-claim or counterclaim in a lawsuit) alleging that the Work
or a Contribution incorporated within the Work constitutes direct
or contributory patent infringement, then any patent licenses
granted to You under this License for that Work shall terminate
as of the date such litigation is filed.

4. Redistribution. You may reproduce and distribute copies of the
Work or Derivative Works thereof in any medium, with or without
modifications, and in Source or Object form, provided that You
meet the following conditions:

(a) You must give any other recipients of the Work or
Derivative Works a copy of this License; and

(b) You must cause any modified files to carry prominent notices
stating that You changed the files; and

(c) You must retain, in the Source form of any Derivative Works
that You distribute, all copyright, patent, trademark, and
attribution notices from the Source form of the Work,
excluding those notices that do not pertain to any part of
the Derivative Works; and

(d) If the Work includes a "NOTICE" text file as part of its
distribution, then any Derivative Works that You distribute must
include a readable copy of the attribution notices contained
within such NOTICE file, excluding those notices that do not
pertain to any part of the Derivative Works, in at least one
of the following places: within a NOTICE text file distributed
as part of the Derivative Works; within the Source form or
documentation, if provided along with the Derivative Works; or,
within a display generated by the Derivative Works, if and
wherever such third-party notices normally appear. The contents
of the NOTICE file are for informational purposes only and
do not modify the License. You may add Your own attribution
notices within Derivative Works that You distribute, alongside
or as an addendum to the NOTICE text from the Work, provided
that such additional attribution notices cannot be construed
as modifying the License.

You may add Your own copyright statement to Your modifications and
may provide additional or different license terms and conditions
for use, reproduction, or distribution of Your modifications, or
for any such Derivative Works as a whole, provided Your use,
reproduction, and distribution of the Work otherwise complies with
the conditions stated in this License.

5. Submission of Contributions. Unless You explicitly state otherwise,
any Contribution intentionally submitted for inclusion in the
Work by You to the Licensor shall be under the terms and
conditions of this License, without any additional terms or
conditions. Notwithstanding the above, nothing herein shall
supersede or modify the terms of any separate license agreement
you may have executed with Licensor regarding such Contributions.

6. Trademarks. This License does not grant permission to use the
trade names, trademarks, service marks, or product names of the
Licensor, except as required for reasonable and customary use in
describing the origin of the Work and reproducing the content of
the NOTICE file.

7. Disclaimer of Warranty. Unless required by applicable law or
agreed to in writing, Licensor provides the Work (and each
Contributor provides its Contributions) on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied, including, without limitation, any warranties or
conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or
FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for
determining the appropriateness of using or redistributing the
Work and assume any risks associated with Your exercise of
permissions under this License.

8. Limitation of Liability. In no event and under no legal theory,
whether in tort (including negligence), contract, or otherwise,
unless required by applicable law (such as deliberate and grossly
negligent acts) or agreed to in writing, shall any Contributor be
liable to You for damages, including any direct, indirect, special,
incidental, or consequential damages of any character arising as a
result of this License or out of the use or inability to use the
Work (including but not limited to damages for loss of goodwill,
work stoppage, computer failure or malfunction, or any and all
other commercial damages or losses), even if such Contributor
has been advised of the possibility of such damages.

9. Accepting Warranty or Additional Liability. While redistributing
the Work or Derivative Works thereof, You may choose to offer,
and charge a fee for, acceptance of support, warranty, indemnity,
or other liability obligations and/or rights consistent with this
License. However, in accepting such obligations, You may act only
on Your own behalf and on Your sole responsibility, not on behalf
of any other Contributor, and only if You agree to indemnify,
defend, and hold each Contributor harmless for any liability
incurred by, or claims asserted against, such Contributor by reason
of your accepting any such warranty or additional liability.

END OF TERMS AND CONDITIONS

---

## Encrypted Local Database (SQLCipher)

### SQLCipher (ZETETIC LLC)

- Dependency: `net.zetetic:sqlcipher-android:4.13.0`
- Repository: https://github.com/sqlcipher/sqlcipher-android
- License: BSD-3-Clause
- Copyright: ZETETIC LLC

---

Copyright (c) 2008-2023, ZETETIC LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the ZETETIC LLC nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY ZETETIC LLC ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL ZETETIC LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
