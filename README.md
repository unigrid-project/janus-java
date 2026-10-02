The Janus Wallet © Stiftelsen The Unigrid Foundation
=====================================================
<img align="right" width="300px" height="auto" src="documentation/janus-logo.png" alt="Janus">

[![Janus build status](https://github.com/unigrid-project/janus-java/actions/workflows/maven.yml/badge.svg)](https://github.com/unigrid-project/janus-java/actions/workflows/maven.yml)
[![Latest release](https://img.shields.io/github/v/release/unigrid-project/janus-java)](https://github.com/unigrid-project/janus-java/releases/latest)
[![Test coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Funigrid-project%2Fjanus-java%2Fbadges%2Fcoverage.json)](https://github.com/unigrid-project/janus-java/actions/workflows/maven.yml)


### About Unigrid
For more information, as well as an immediately useable, binary version of the Unigrid software, see https://www.unigrid.org

### License
Janus is released under the terms of an addended GNU Affero GPL license version 3. See [COPYING](COPYING) and [COPYING.addendum](COPYING.addendum) for more information.

### Development process
Developers work in their own trees, then submit pull requests when they think their feature or bug fix is ready.

The patch will be accepted if there is broad consensus. Developers should expect to rework and resubmit patches if the code doesn't match the coding conventions or level of quality of the project.

The `master` branch is regularly built and tested, but is not guaranteed to be completely stable. [Tags](https://github.com/unigrid-project/janus-java/tags) are created regularly to indicate new official, stable release versions.

### Building
```
mvn clean install
```

This builds and tests the `core`, `web`, `ui` and `shell` modules: the Hedgehog
client, the embedded web server, the server-rendered pages and the window that
hosts them.

The `desktop` module builds the native installers and is only part of the build
with the `installer` profile. It needs JDK 25 and the platform's packaging tools
(`dpkg-deb`, `fakeroot` and `rpm` on Linux, WiX 3 on Windows, Xcode's command
line tools on macOS):
```
mvn -Pinstaller verify -DskipTests
desktop/collect-installers.sh <version>
```

The installers carry their own Java runtime and the browser engine, so an
installed Janus starts without downloading anything but Hedgehog. The build
fetches the engine once into `desktop/target/input/jcef`; `mvn clean` removes
it. The installers end up in `desktop/target/release`. The previous JavaFX
implementation, together with its release workflows, remains available on the
`legacy-javafx` branch.

### Releasing
On an up to date `master` whose build checks have passed, run `./create-release.sh [version [next]]`. It makes
one commit that sets the release version, tags it, and makes one more that moves on to the next snapshot. The
version defaults to the current snapshot without its suffix, and the next one to the following patch number.
Nothing is pushed. When the commits look right, `git push --atomic origin master v<version>` starts the
release workflow, which builds and tests the installers and leaves a draft release to review and publish.

## Running
```
mvn install -DskipTests
mvn -pl shell exec:java
```

This opens the Control Center in its own window. Once a wallet has been
chosen, Janus remembers its backup copy in `~/.janus/wallet` and opens on that
wallet's Dashboard at the next start; deleting the file starts over at
Welcome. The wallet is shown read-only from Hedgehog's frozen legacy ledger, so
Hedgehog must be on the `PATH`, or be named with `-Djanus.hedgehog=<path>`.

Templates are resolved from the classpath with caching disabled, so running
from a development classpath picks up an edited template on the next request
without a restart.

### Automated Testing
Developers are strongly encouraged to write unit tests for new code, and to submit new unit tests for old code.

`mvn install` runs the unit tests together with the flow tests in `ui`, which
serve the real controllers and templates and walk through them over HTTP.

The browser tests in `e2e` drive the same interface in headless Chromium and
are only built with the `browser` profile. Playwright downloads Chromium on the
first run:
```
mvn install -DskipTests
mvn -Pbrowser -pl e2e verify
```

On a distribution Playwright does not recognise, such as Linux Mint Debian
Edition, name the closest supported one, for example
`PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=debian12-x64`.
