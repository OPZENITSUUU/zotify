# Zotify Music Finalization Handoff

Last updated: 2026-11-19 18:41 IST

## Repository state

Repository: [OPZENITSUUU/zotify-music](https://github.com/OPZENITSUUU/zotify-music). Local branch: `fix/finalization-audit-2026-11-18`, current commit `2202311`. The branch includes the latest `main` commit (`aaeb249`) and latest PR #1 head (`1b24a91`) as ancestors; the merge conflicts were resolved locally. PR #1 has not been updated on GitHub yet. No PR has been merged by this work, and no tag or release was created.

The generated `FINALIZATION.patch` is being rebuilt with color-disabled Git output and will be checked with `git apply --check` against the latest PR #1 base before delivery.

## Production compatibility and signing evidence

Public Releases currently contain v1.0.1, v1.0.2 and v1.0.3. Their source tags declare phone package `com.zotifymusic.player` and TV package `com.zotifymusic.player.tv`; the Wear OS companion matches the published phone/watch identity. The public v1.0.2 and v1.0.3 phone APKs were verified with `apksigner`, and their package/certificate matches the current `main` production identity. The v1.0.3 APK SHA-256 matched GitHub's public release-asset digest. The provided signing backup was handled in private tmpfs only; the keystore was usable, and its certificate matched current `main` and the published v1.0.3 APK. It does not match the stale pin in the older PR #1 source.

The local branch retains the released package IDs and current production signing pin. No signing credential or keystore was printed, committed, or uploaded. GitHub Actions signing secrets remain unconfigured; the release gate remains disabled. Do not revert to `com.voidmusic.player` or the former certificate pin.

The next candidate metadata is phone v1.0.12/code 43 and TV v1.9/code 27, both strictly above the current published release metadata. This was validated locally; no tag was created.

## Fixes and tests

The finalization covers phone updater URL/asset validation, SHA-256 checking, resumable downloads, installed package/version/signing-lineage checks, optional dismissal persistence and mandatory-update protection; phone and TV-specific release metadata and APK names; fail-closed release policy/publication checks; Wear APK/checksum naming and identity; and Android, TV, Wear OS, Desktop, web and documentation branding. Existing user-data paths and stable internal identifiers are preserved.

Historical API 30 evidence was retrieved from the uploaded JUnit artifact for the failed PR #2 run. It identified `MediaController.getPlaybackState()` at `PlaybackPipelineFlacInstrumentedTest.kt:115` throwing “MediaController method is called from a wrong thread.” The test now binds the `MediaBrowser` explicitly to `Looper.getMainLooper()`. PR #1's separate API 30 failure was ADB “Failure calling service settings: Broken pipe”; the emulator workflow retains the tested `disable-animations: false` setting that skips those optional ADB writes. The workflow still runs the playback test and now also runs an updater persistence regression; `set -euo pipefail` ensures either failure fails the required job.

Verified locally on the merged branch:

- Release validator unit tests: 15 passed.
- Validator for tag candidate v1.0.12 returned phone code 43 and TV code 27; no tag/release was created.
- Desktop, Desktop UI, Shared UI and Shared JVM tests: `BUILD SUCCESSFUL` (51 actionable tasks; 28 executed, 23 up-to-date).
- Workflow YAML parse: all 9 workflow files passed.
- Android/TV/Wear/Desktop XML parse: all 153 resource files passed.
- Production package IDs and release-workflow signing pin matched current `main`.
- `git diff --check` and merge-marker checks passed.
- Public release APK package/signature/digest checks passed as above.

The complete `:shared:allTests` invocation cannot run in this sandbox: it selects `:shared:testDebugUnitTest`, which requires an Android SDK. Android/Kotlin compilation and the API 30 emulator tests also cannot run locally because `ANDROID_HOME` and an SDK are absent. Go is not installed and the repository requires Go 1.27.0. The Android instrumentation test (including the new updater persistence test), Android/TV builds, and Go tests must be verified by GitHub Actions after pushing the updated PR #1 head.

## Remaining actions

1. Regenerate `FINALIZATION.patch` without color escapes and verify it applies cleanly to the latest PR #1 head; scan the patch for credential material and confirm no keystore files are present.
2. Run final local static and release tests; confirm the worktree is clean apart from the downloadable patch.
3. Fast-forward the existing PR #1 branch with the local merge/fix commits (no force push), then inspect Android, API 30 playback/updater, TV, Desktop and Go Actions results. Fix real failures; do not bypass required checks.
4. Do not merge PRs, create tags or publish releases. Those actions remain expressly out of scope until all required checks pass and a later release is authorized.
