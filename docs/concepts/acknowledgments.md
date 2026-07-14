---
title: Acknowledgments
description: The projects, people, and prior art the GlycemicGPT mobile app's pump and CGM drivers stand on.
---

GlycemicGPT exists because of work that came before it. This page acknowledges the projects whose code, research, or community advocacy directly inform the pump and CGM drivers shipping in the GlycemicGPT mobile app. None of the projects below are affiliated with GlycemicGPT; we're listing them because their work made ours possible.

## Pump and CGM library credits

This project's diabetes-device integrations are built on top of -- or directly informed by -- several open-source libraries (MIT- and GPL-licensed). The categorization below distinguishes "runtime dependency" (we ship and consume the library directly) from "architectural reference" (we studied the work to build our own, no code is imported).

### James Woglom ([@jwoglom](https://github.com/jwoglom))

Two of jwoglom's open-source projects directly inform our Tandem support:

- **[pumpX2](https://github.com/jwoglom/pumpx2)** -- *architectural reference, not a runtime dependency.* Java library implementing a reverse-engineered Bluetooth protocol for Tandem t:slim X2 / Mobi pumps. GlycemicGPT's Tandem mobile-app driver is an independent Kotlin port informed by pumpX2's protocol documentation, opcodes, message formats, and EC-JPAKE authentication flow. We do not import pumpX2; we use its test vectors to validate parser correctness in our own code. Crediting this work is required by the MIT license and matters: without pumpX2's published reverse-engineering, this project's pump driver would not exist.
- **[controlX2](https://github.com/jwoglom/controlx2)** -- *architectural reference, not a runtime dependency.* Android / Wear OS reference app built on pumpX2. We studied its BLE service lifecycle, reconnection state machines, and pairing flow patterns. No code is imported.

Both are MIT-licensed by James Woglom. Per-library attribution lives in [`docs/THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md) in this repo.

In-source headers also reference the upstream MIT license in the relevant Tandem driver files (`TandemProtocol.kt`, `JpakeAuthenticator.kt`, `EcJpake.kt`, `Hkdf.kt`).

If you're using GlycemicGPT's Tandem integration, you're benefiting from years of jwoglom's reverse-engineering work. We are deeply grateful and try to credit accurately. If anything on this page or in the per-package license file reads as inadequate or wrong, please [open an issue](https://github.com/GlycemicGPT/android-unofficial/issues/new/choose) -- correctness matters.

### palmarci ([@palmarci](https://github.com/palmarci)) and the OpenMinimed project

The Medtronic MiniMed **on-device Bluetooth** driver (which talks to a 680G / 770G / 780G pump directly, no cloud) exists because of [OpenMinimed](https://github.com/OpenMinimed) -- a community project that reverse-engineered the MiniMed 700-series BLE protocol, including the SAKE authenticated key exchange and the read-only data surface (sensor glucose, insulin-on-board, basal, bolus history, reservoir, battery).

This is the **one place GlycemicGPT differs from its Tandem posture.** Where the Tandem driver is an independent reimplementation that imports no upstream code, the Medtronic BLE driver is a **direct dependency / port** of OpenMinimed:

- **[JavaSake](https://github.com/OpenMinimed/JavaSake)** -- ***runtime dependency.*** OpenMinimed's production-grade SAKE handshake is consumed from Maven Central (`org.openminimed:javasake`, package `org.openminimed.sake`) and compiled into the app, driven through `MedtronicSakeSession`.
- **[PythonPumpConnector](https://github.com/OpenMinimed/PythonPumpConnector)** -- ***ported, not merely referenced.*** The CGM, IDD-status, history, device-info, and battery readers are ported line-for-line into Kotlin from this project.
- **[PythonSake](https://github.com/OpenMinimed/PythonSake)** and **[JavaPumpConnector](https://github.com/OpenMinimed/JavaPumpConnector)** -- the SAKE reference and the Android BLE peripheral scaffolding that informed the connection manager.

This is possible because **OpenMinimed is GPL-3.0 and GlycemicGPT is itself GPL-3.0**, so the licenses are compatible and copyleft propagation is a non-issue. The work is used **with the explicit permission of the author, palmarci (Pál Marci)**, who relicensed OpenMinimed to GPL-3.0 for this purpose. Upstream copyright and GPL-3.0 notices are retained verbatim in the published artifact and every ported file, and in-source headers cite the specific upstream source file each port derives from. The project credits **palmarci** as primary author and reverse-engineer, contributors **drfubar**, **Morten Fyhn Amundsen**, and **Stenium**, and the original `medtronic-bt-decrypt` proof-of-concept by **[@planiitis](https://github.com/planiitis)**. The Android/JVM ports of JavaSake and JavaPumpConnector are maintained by the GlycemicGPT project lead under [jlengelbrecht](https://github.com/jlengelbrecht).

Without OpenMinimed's published reverse-engineering, the Medtronic on-device driver would not exist. Per-package credit lives in [`docs/THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md) in this repo.

## The diabetes-OSS movement

GlycemicGPT is part of a much larger #WeAreNotWaiting tradition that has been building open-source diabetes tools collaboratively since around 2013. The projects below predate this one by years; in many cases by a decade. We're listed alongside them, not above them.

- **[Nightscout](https://nightscout.github.io/)** -- the open-source CGM dashboard that effectively defined the "self-host your own diabetes data" pattern. The fact that this is even a category that exists is because of Nightscout. The maintainers and the [Nightscout Foundation](https://www.nightscoutfoundation.org/) deserve credit for building and sustaining the movement that GlycemicGPT slots into.
- **[OpenAPS](https://openaps.org/)** and Dana Lewis -- the project that started the closed-loop branch of #WeAreNotWaiting. GlycemicGPT does not do closed-loop and never will, but the broader tradition of "T1s building safety-critical software for themselves" comes from here.
- **[Loop](https://loopkit.github.io/loopdocs/)** and the LoopKit / Pete Schwamb -- the iOS closed-loop project that brought thousands of T1s and parents into the DIY closed-loop world. GlycemicGPT respects the legal and architectural posture (forks-as-personal-medical-device) that Loop established.
- **[AndroidAPS / AAPS](https://androidaps.readthedocs.io/)** -- the Android closed-loop project that supports the broadest pump matrix in the open-source space. The plugin architecture in this app borrows conceptual framing from AAPS's pump-driver model.
- **[xDrip+](https://github.com/NightscoutFoundation/xDrip)** -- the Android CGM relay-and-dashboard that supports more sensors than anything else. The "what should a CGM-companion app on Android even look like" question is largely answered by xDrip+; we benefit from the answer.
- **[Tidepool](https://www.tidepool.org/)** -- a nonprofit cloud platform for uploading and reporting on diabetes data, and the FDA-cleared variant of Loop ([Tidepool Loop](https://www.tidepool.org/tidepool-loop)). The "free, open, vendor-neutral diabetes data layer" pattern is something GlycemicGPT learns from.

## Project lead's note

The GlycemicGPT mobile app would not be possible without the work above. If you've contributed to any of the listed projects and feel your work should be called out differently, please reach out -- correctness here matters more than concision.

If you're a T1 or caregiver who got value out of any of the upstream projects, please consider contributing back to them as well. The diabetes-OSS world stays alive because users contribute time, code, and donations to it -- the [Nightscout Foundation](https://www.nightscoutfoundation.org/donate) in particular accepts donations and sustains a lot of the infrastructure the rest of us build on.
