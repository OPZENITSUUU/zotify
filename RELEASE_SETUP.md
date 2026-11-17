# Android release setup

The production Android signing identity was rotated on 2026-11-17. The current certificate SHA-256 fingerprint is `EAD01DEB09889B57D7C9E16E52811CE02E85754BD6C19A483AAF772A59C7EE30`. Any APK installed with a different signing certificate cannot be updated in place with this identity. Users must back up any needed app data, uninstall the old app, then install the new APK. Android may delete app-local data during uninstall. After that reinstall, future releases signed with this identity can update normally, subject to user approval.

At the time of this rotation, this repository had no published GitHub Releases. APKs distributed outside GitHub may still have another signing identity and require the reinstall described above.

## Configure GitHub Actions

Before running a signed build, add these **repository Actions secrets** in `OPZENITSUUU/zotify-music`:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Use the matching values from the private signing backup. For `KEYSTORE_BASE64`, copy only the value after `KEYSTORE_BASE64=`. Do not commit the keystore, password values, or handoff file, and do not paste them into workflow logs or issues. Keep an offline backup of the keystore and its passwords; losing this key will require another user reinstall.

The workflows verify that the decoded keystore opens, the alias and key password work, and the certificate SHA-256 fingerprint matches `EAD01DEB09889B57D7C9E16E52811CE02E85754BD6C19A483AAF772A59C7EE30` in both Android signing workflows. A different key must not be substituted without an explicit migration plan.

## Publish a release

Push a version tag such as `v1.0.12` after the tag, `versionName`, and Android `versionCode` have been reviewed. The release workflow validates the tag and code, checks release ordering and duplicate status, runs Android unit tests, verifies signing, builds the production APK, checks its package/version/signature/alignment, and publishes the APK plus its SHA-256 checksum. It fails before publishing if signing secrets are absent or invalid.

The in-app updater reads the numeric `Android version code: N` line in the GitHub Release body. It will not offer a build with a lower version code; a same-code beta-to-stable transition remains supported through version-name ordering. Future release notes must keep that metadata line intact.
