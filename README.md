The Janus Wallet © Stiftelsen The Unigrid Foundation
=====================================================
<img align="right" width="300px" height="auto" src="documentation/janus-logo.png" alt="Janus">

[![Janus build status](https://github.com/unigrid-project/janus-java/actions/workflows/maven.yml/badge.svg)](https://github.com/unigrid-project/janus-java/actions/workflows/maven.yml)


About Unigrid
-------------
For more information, as well as an immediately useable, binary version of the Unigrid software, see https://www.unigrid.org

License
-------
Janus is released under the terms of an addended GNU Affero GPL license version 3. See [COPYING](COPYING) and [COPYING.addendum](COPYING.addendum) for more information.

Development process
-------------------
Developers work in their own trees, then submit pull requests when they think their feature or bug fix is ready.

The patch will be accepted if there is broad consensus. Developers should expect to rework and resubmit patches if the code doesn't match the coding conventions or level of quality of the project.

The `master` branch is regularly built and tested, but is not guaranteed to be completely stable. [Tags](https://github.com/unigrid-project/janus-java/tags) are created regularly to indicate new official, stable release versions.

Design documents
----------------
* [Frontend migration](documentation/frontend-migration.md) — proposal to replace the JavaFX user interface with server-rendered HTML and CSS in an embedded browser.

Building
--------
```
mvn clean install
```

This builds the `core`, `web` and `shell` modules. Note that the modules are
currently empty scaffolding: the JavaFX implementation has been removed and the
replacement described under [Design documents](#design-documents) has not been
written yet.

The `desktop` module holds the jlink and jpackage configuration that produces
the native installers. It is deliberately kept outside the reactor until `shell`
provides an application entry point, so no installer can be produced from this
branch. The previous JavaFX implementation, together with its release
workflows, remains available on the `legacy-javafx` branch.

Running
-------
```
mvn install -DskipTests
mvn -pl shell exec:java
```

This starts the interface on a loopback address and logs the URL to open;
the port is assigned at startup and changes between runs. The embedded
browser window that will eventually host it does not exist yet, so point an
ordinary browser at the logged address.

Templates are resolved from the classpath with caching disabled, so running
from a development classpath picks up an edited template on the next request
without a restart.


Troubleshooting
---------------
If you are running into issues starting the wallet a good place to look is our [documentation](https://docs.unigrid.org/) page.


Automated Testing
-----------------
Developers are strongly encouraged to write unit tests for new code, and to submit new unit tests for old code.
