# Fylio APK downloads

Build artifacts land here for quick download from the workspace. They are **not** tracked
in this branch (they would add ~30 MB to the repo and they are reproducible):

- `app-release.apk` — 12 MB, minify off, signed with the auto-generated debug key, installs
  on any device via sideload or `adb install -r app-release.apk`.
- `app-debug.apk` — 18 MB, debug build with extra metadata.

The canonical, always-current copies live on the **`apk-latest`** branch, refreshed by
`.github/workflows/fylio-apk.yml` on every push that touches `fylio/`:

```
https://github.com/ashishshetty472-netizen/odin-recipies/raw/apk-latest/app-release.apk
```

To rebuild from source: push a change under `fylio/`, or run the *Build Fylio APK*
workflow manually from the Actions tab.
