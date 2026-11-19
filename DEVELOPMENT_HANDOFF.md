# Zotify Music Finalization Handoff

Last updated: 2026-11-19 18:30 IST

## Repository state

Repository: [OPZENITSUUU/zotify-music](https://github.com/OPZENITSUUU/zotify-music). Work is local on `fix/finalization-audit-2026-11-18`; there is an in-progress local merge based on an older PR #1 head and `main` commit `0a152a0`. Current remote refs have since advanced: `main` is `aaeb249`, and PR #1 head is `1b24a91`. PR #1 currently reports `CONFLICTING` / `DIRTY` and has no checks listed on its latest head. No PR has been merged, no tag created, and no release published by this task.

The local changes have not yet been pushed. Before pushing, the branch must incorporate current `main` and the seven newer commits on PR #1 without dropping either side.

## Production compatibility and signing evidence

Public Releases currently contain v1.0.1, v1.0.2, and v1.0.3. Their source tags declare the phone package ID `com.zotifymusic.player`, TV ID `com.zotifymusic.player.tv`, and Wear OS package matching the phone/watch identity. The v1.0.2 and v1.0.3 public phone APKs were verified with `apksigner`; their package/certificate matches the current `main` production identity. The v1.0.3 APK SHA-256 also matched GitHub's public release-asset digest. The supplied signing backup was inspected only in private tmpfs; its keystore/alias/password were usable, and its certificate matched current `main` and the published v1.0.3 APK. It does not match the stale certificate pin in the older PR #1 source branch.

The branch is being aligned to the actual published package IDs and current `main` certificate pin. Do not revert to `com.voidmusic.player` or the former pin. Signing credentials were not printed, committed, or uploaded; GitHub Actions signing secrets remain unconfigured, and the release gate is not enabled.

## Local changes and verification

The finalization changes cover the phone updater's official asset URL/digest selection, resumable download verification, install package/version/signature checks, optional dismissal and mandatory-update behavior; TV-specific version metadata, APK naming and temporary-hide versus explicit deferral; Wear APK asset/checksum naming; release-policy parsing and validation; Android/TV/Wear package identity; and user-visible Android, TV, Wear OS, Desktop, web, and documentation branding. App IDs and user-data paths are retained from the published production lineage.

A new Android instrumentation regression test covers optional update dismissal persistence and mandatory-update dismissal immunity. The required API 30 emulator job keeps its FLAC playback regression test and is extended to run this updater test; neither required check is bypassed.

Verified locally so far:

- `.github/scripts/test_validate_release.py`: 15 tests passed.
- `validate_release.py --tag v1.0.12`: validated phone code 43 and TV code 27 against current published release ordering; no tag was created.
- YAML parsing: all 9 GitHub workflow files parsed.
- XML parsing: 153 Android, TV, Wear OS and Desktop resource files parsed.
- Production package IDs and workflow signing pins matched current `main` in the local working tree.
- Desktop Gradle test: `:desktopApp:test` completed successfully after fixing a branding replacement that had changed an internal icon identifier.
- Public v1.0.2 and v1.0.3 APK signature/package checks passed as described above.
- `git diff --check` passed on the current changes.

Not verified locally: Android/Kotlin compilation or emulator tests, because no Android SDK is installed and Gradle omits Android modules in this sandbox; Go tests, because Go is not installed. These must be covered by GitHub Actions after the latest PR1/main integration is pushed. The additional Android instrumentation test has not yet run.

## Remaining steps and release gate

1. Finish the local merge with current `main` and the latest PR #1 head, preserving current package IDs, signing pin, R8 version, mainline branding, PR1 workflow permission limits, trusted updater URL checks, and Desktop updater tests.
2. Re-run release validator, static parsing, Desktop/shared JVM tests and `git diff --check`; regenerate `FINALIZATION.patch` and validate it with `git apply --check` against the PR base.
3. Push the updated PR #1 branch without force-pushing. Wait for Android, API 30 playback, TV, Desktop, and Go required checks; inspect logs and fix any actual failures. Do not bypass required checks.
4. Do not merge PRs, create tags, or publish releases. The release gate remains off and signing secrets are still unconfigured; release requires a later authorized action after all checks pass.
