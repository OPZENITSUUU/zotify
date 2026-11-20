# Android release setup

The Android source currently identifies itself as v1.0.12 (versionCode 43). The published v1.0.1, v1.0.2, and v1.0.3 phone releases use `com.zotifymusic.player`; their release lineage must remain the compatibility baseline. The v1.0.2 and v1.0.3 APK signatures were verified against the current `main` certificate pin, and the provided backup keystore was verified to match that pin and the published v1.0.3 signer. The phone, TV, and Wear OS Gradle IDs and release checks in this branch must match the current production IDs; do not restore the stale `com.voidmusic.player` IDs or the former certificate pin from the older PR branch. This audit did not upload the backup or configure Actions secrets.

## Configure GitHub Actions

Before a future signed release, an authorized maintainer must configure these **repository Actions secrets** in `OPZENITSUUU/zotify-music` through GitHub's secure secret settings. They are currently not configured in this repository:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Use only the already-verified production keystore and matching values from an authorized private vault. Never commit, print, log, or expose the keystore, passwords, or Base64 data in chat, workflow output, issues, or repository files. Keep a separately secured offline backup. The supplied backup was verified locally only and was not uploaded.

The release workflow verifies that the decoded keystore opens, the alias and key password work, and the certificate SHA-256 fingerprint matches the current production pin. A mismatch must fail closed; do not change the pin to make another key pass. Preserve the production phone ID `com.zotifymusic.player`, TV ID `com.zotifymusic.player.tv`, and their associated app data and updater paths.

## Publish a release

Do not push a version tag or use workflow dispatch yet. The current repository release gate remains disabled, Actions signing secrets are not configured, and this finalization PR has not passed its required CI. A later release requires every required check to pass on the final PR head and explicit maintainer release approval. The release workflow validates tag/version ordering; runs Android unit tests; verifies the pinned identity; builds and inspects phone and TV production APKs; and publishes matching checksum assets. It fails closed on absent secrets or mismatched signatures, IDs, versions, or assets.

The phone updater reads `Android version code: N`; the TV updater reads `Android TV version code: N` and `Android TV version name: X`. Each client compares its own module's version metadata and only selects its own compatible APK asset. Release notes must keep these metadata lines intact; phone mandatory-update policy applies only to phone builds.

## Optional mandatory-update policy

`.github/release-policy.json` sets `minimum_supported_android_phone_version_code`. The checked-in default is `0`, which means updates remain optional. For a future phone release that must require a newer minimum, set this to a positive Android phone versionCode no greater than the release's own versionCode in the tagged source. The release workflow validates the value and writes `Minimum supported Android phone version code: N` into the official release notes. The phone app treats only one positive, valid threshold that does not exceed the release versionCode as mandatory; missing or malformed threshold metadata is treated as optional. TV builds ignore this phone-only threshold. A mandatory notice has no Later action, but downloading and installation still require the user's approval through Android.
