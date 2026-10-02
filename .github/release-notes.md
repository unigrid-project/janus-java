## Installing

Every installer carries its own Java runtime and browser engine. Check a download against `SHA256SUMS`
before installing it:

```
sha256sum --check --ignore-missing SHA256SUMS
```

These installers are not signed with a certificate from Apple or Microsoft, so each system warns before
it runs one for the first time:

- **macOS:** open the `.dmg`, drag Unigrid to Applications and open it once. When macOS refuses, go to
  System Settings > Privacy & Security and choose **Open Anyway**.
- **Windows:** when SmartScreen stops the installer, choose **More info** and then **Run anyway**.
- **Linux:** install the `.deb` or `.rpm` with the package manager, or unpack the `.tar.gz` anywhere.
