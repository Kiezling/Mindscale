# MindScale private APK beta

This handout is for a private APK test only. It does not publish the app, create a public download
URL, or authorize a store upload.

## Install

1. Receive `app-debug.apk` through the private delivery route chosen by the owner.
2. Open the APK from the browser or file manager used to receive it. Android may ask you to allow
   that specific browser or file manager to install unknown apps; allow it only if you trust that
   source, then return to the APK and open it.
3. Review the installer and choose Install. Launch MindScale from the installer or app list.

MindScale requires Android 8.0 (API 26) or newer. Open the `MindScale` app entry. The debug build
also includes a development-only `DesignGalleryActivity` that has no launcher icon; developers can
open it with an explicit activity launch. A release build omits that gallery.

The current candidate is `app/build/outputs/apk/debug/app-debug.apk`, signed by the existing local
debug key. A later release needs an owner-approved durable signing identity and reproducible version
and update decisions. Do not treat this debug APK as a production release.

## Manual smoke paths

Exercise first launch and offline use, then Track, Full Log, Insights, Profile, Report, Settings,
and the Safety card. In Settings, read the privacy/product information, change a preference, export
JSON and CSV to a location you choose, and test the import preview without confirming destructive
actions unless you intend to do so. Check export-then-erase confirmation and cancellation. Exercise
back navigation, process recreation, light/dark themes, 100–200% font sizes, and TalkBack where
available. Use Safety actions only as UI checks; do not contact a crisis service as a test.

## Private feedback

Send reports through the private route supplied by the owner. Include steps to reproduce, device
model, Android version, app version, and whether a fresh launch changes the result. Do not include
health details, symptom records, notes, safety-plan text, phone numbers, or screenshots containing
them. Redact any personal information before sending a report.

## Data and updates

Android automatic backup is disabled. Export a JSON backup before uninstalling or changing devices;
uninstalling can remove local data, and exported files are unencrypted. An APK update should retain
app data when Android accepts the same signing identity, but verify an export before updating and do
not assume recovery from an uninstall. The owner must settle durable signing and reproducible
version/update decisions before broader distribution.
