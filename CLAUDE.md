# MindScale Android development rules

- Treat the generated Kotlin/Compose starter as the compatibility baseline. Keep Kotlin DSL, Jetpack Compose, Material 3, minimum SDK 26, and the template-selected compile/target SDK unless an explicit upgrade is requested.
- Use `./gradlew` or `.\gradlew.bat`; never install or invoke a system Gradle. Use Android Studio's bundled JDK through `JAVA_HOME` and the SDK through `ANDROID_HOME`.
- Do not change Gradle, AGP, Kotlin, Compose, or Java versions independently. Upgrade them only as a mutually compatible set with an explicit reason.
- Never create or commit production signing keys, credentials, API keys, `local.properties`, IDE-local state, build outputs, APKs, or AABs.
- Prefer small, reviewable changes. Do not delete data, rewrite Git history, publish builds, or change signing/release configuration without approval.
- Before handing off code changes, run `.\gradlew.bat test`, `.\gradlew.bat lint`, and `.\gradlew.bat assembleDebug`. Run `.\gradlew.bat clean` when validating environment or cache-related changes.
- Before device work, confirm `adb devices` reports the intended device. Verify user-visible changes by launching the debug build on the configured API 36 emulator.
