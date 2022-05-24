The Janus Wallet © The Unigrid Foundation
=========================================
<img align="right" alt="Janus cryptocurrency wallet" src="https://upload.wikimedia.org/wikipedia/commons/a/a4/Meyers_b9_s0153_b1.png" width="300"/>

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

Building
--------
To build a working package, you first need to create an app-image that can hold the native binaries of the Unigrid daemon;
```
mvn clean package # will build a jlink distribution and an app-image
```
Next, you should copy the unigrid daemon executables for your operating system into `desktop/target/dist/janus/bin/`. This can
be done by GitHub Actions as an intermediate step. However, doing it by hand also works just fine.

Finally, we create the actual installer image with everything;
```
cd desktop
mvn jpackage:jpackage@installer
```

The resulting installer image is placed under `desktop/target/dist/`.

Automated Testing
-----------------
Developers are strongly encouraged to write unit tests for new code, and to submit new unit tests for old code.
