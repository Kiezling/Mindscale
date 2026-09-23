# MindScale upload-key custody

Created 23 September 2026 at the owner's request. This is an **upload key** for Google Play;
it is not the debug key or the Google-managed key used to sign installed Play apps.

- Keystore: `C:\Users\mckie\.android\MindScale-release\mindscale-upload.p12`
- Alias: `mindscale-upload`; PKCS12; RSA 4096; validity through February 2054.
- Public certificate: `mindscale-upload-cert.pem` in the same folder.
- Certificate SHA256: `12:53:52:97:39:7D:B2:1D:78:44:69:A3:C7:DC:C1:7B:F5:95:9F:19:19:02:44:84:C7:92:AE:16:ED:D7:FE:D2`.
- Random password: Windows current-user DPAPI-protected file `upload-password.dpapi` in
  the same directory. Folder inheritance is disabled; only the current Windows user and SYSTEM
  have explicit access. No plaintext password is in the repository, logs or chat.

## Backup still required

An off-device backup has **not** been made. Preserve the keystore and recoverable password
in the owner's password manager or encrypted backup. Copying only the DPAPI file to another
computer does not make the password portable: its decryption depends on this Windows profile.
Never publish any keystore/password file or paste a password into an agent conversation.

The owner can retrieve the password locally for secure password-manager storage, in their own
private PowerShell session (the agent must not run this display command):

```powershell
$protectedPasswordPath = Join-Path $env:USERPROFILE '.android/MindScale-release/upload-password.dpapi'
$uploadSecret = (Get-Content -LiteralPath $protectedPasswordPath -Raw).Trim() | ConvertTo-SecureString
[System.Net.NetworkCredential]::new('', $uploadSecret).Password
```

Clear/close that private terminal after saving it securely. Keep recovery access to the Play
owner account. Play supports an upload-key reset if the upload key is lost; this is distinct
from replacing the app signing key. See https://developer.android.com/studio/publish/app-signing.

## Build and signing workflow

The project's Gradle release bundle remains unsigned by design; no secrets were added to Gradle.
Build using the existing wrapper/toolchain, then sign a copy with `jarsigner` and the upload key.
The current verified invocation lives in ignored `build/review/play-submission/sign-release.ps1`.
It decrypts the password only in-process, passes it through a temporary environment variable,
clears that variable on exit, verifies the signed JAR and prints only the artifact hash.
It refuses to overwrite an existing candidate. The signing script trims the DPAPI file's
trailing newline; without Trim, PowerShell cannot parse the protected value.

Before uploading, verify the certificate and version. After an upload, treat versionCode as
consumed; record the exact uploaded SHA256. Do not use the debug signing identity for Play.

The owner's debug installation will not normally accept a differently signed Play update.
Do not uninstall it. Plan a verified JSON backup/restore migration separately, with the owner's
authorization before any action that removes local records.
