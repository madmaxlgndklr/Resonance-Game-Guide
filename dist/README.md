# Test builds

| | |
| --- | --- |
| File | `resonance-guide-v0.1.0-debug.apk` |
| Built from | `v0.1.0` (`8f48f28`) |
| Version | 0.1.0 (versionCode 1) |
| Size | 17.2 MB |
| SHA-256 | `e22564bec0e6115df8e44a3f3feb0d9303efa3bb5d7371c9f548057b665ef579` |
| Min Android | 8.0 (API 26) |

## Installing

Download the `.apk` from this directory on GitHub (open the file, then the
download button) and open it on the phone. Android will ask you to allow
installs from your browser or file manager the first time.

Over adb instead:

```bash
adb install -r dist/resonance-guide-v0.1.0-debug.apk
```

## What to expect

- The guide data is bundled, so everything works offline and the four tabs are
  populated on first launch.
- **No artwork is included.** Maps and sprites show a placeholder; see
  `scripts/guide_pipeline/README.md` for why, and the repository README for how
  to supply your own.
- This is a **debug** build: signed with the local debug key, not optimised or
  minified, and larger and slower than a release build would be. It is for
  testing, not distribution.

## Why the binary is in the repository

It is committed on request so it can be downloaded straight from the repo
without a GitHub login. Two lighter-weight alternatives, for reference:

- every CI run on `main` and on each pull request uploads the same debug APK as
  a build artifact, downloadable from the run's page;
- attaching the APK to a GitHub Release for the tag keeps it off the repository
  history entirely.

A committed binary stays in git history permanently and adds its full size to
every clone, so prefer one of the above for later builds rather than adding an
APK per version here.
