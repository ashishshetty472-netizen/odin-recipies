# Fylio APK downloads

Current build: icon-sheet release (88 tool icons from the supplied sheet + logo launcher icon).

- `app-release.apk` — 13 MB, minify off, signed with the auto-generated debug key.
  Installs on any device via sideload or `adb install -r app-release.apk`.
- `app-debug.apk` — 19 MB, debug build.

These local copies are not tracked in this branch (see `.gitignore`); they are refreshed
from CI after every build. The canonical copies live on the **`apk-latest`** branch:

```
https://github.com/ashishshetty472-netizen/odin-recipies/raw/apk-latest/app-release.apk
```

Rebuild anytime: push a change under `fylio/`, or run *Build Fylio APK* from the Actions tab.
