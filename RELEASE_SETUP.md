# Android release setup

The Android source currently identifies itself as v1.0.12 (versionCode 43) and the production release workflow is configured for a newly generated signing identity. No signed APK release is currently published. When that release is published, existing installs signed with the previous certificate cannot update in place; users must back up any needed data, uninstall the old app, and install the new APK once. Android may remove app-local data during uninstall. After that reinstall, Android can accept future updates signed with the new key, subject to the user's install approval.

## Configure GitHub Actions

Before running a signed build, add these **repository Actions secrets** in `OPZENITSUUU/zotify-music`:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Use the matching values from the private signing handoff. For `KEYSTORE_BASE64`, copy only the value after `KEYSTORE_BASE64=`. Do not commit the keystore, password values, or handoff file, and do not paste them into workflow logs or issues. Keep an offline backup of the keystore and its passwords; losing this key will require another user reinstall.

The workflows verify that the decoded keystore opens, the alias and key password work, and the certificate SHA-256 fingerprint matches the pin in both Android signing workflows. A different key must not be substituted without an explicit migration plan.

## Publish a release

Push a version tag such as `v1.0.12` after the tag, `versionName`, and Android `versionCode` have been reviewed. The release workflow validates the tag and code, checks release ordering and duplicate status, runs Android unit tests, verifies signing, builds the production APK, checks its package/version/signature/alignment, and publishes the APK plus its SHA-256 checksum. It fails before publishing if signing secrets are absent or invalid.

The in-app updater reads the numeric `Android version code: N` line in the GitHub Release body. It will not offer a build with a lower version code; a same-code beta-to-stable transition remains supported through version-name ordering. Future release notes must keep that metadata line intact.
