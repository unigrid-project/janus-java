The Janus Wallet © The Unigrid Foundation
=========================================
<img align="right" alt="Janus cryptocurrency wallet" src="https://upload.wikimedia.org/wikipedia/commons/a/a4/Meyers_b9_s0153_b1.png" width="300"/>

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

Building
--------
To build a working package, you first need to create an app-image that can hold the native binaries of the Unigrid daemon;
```
mvn clean install # will build a jlink distribution and an app-image
```

Finally, we create the actual installer image with everything;
```
cd desktop
mvn jpackage:jpackage@installer
```

The resulting installer image is placed under `desktop/target/dist/`.

Releasing
---------

**This step only needs to be performed once on OSX or Linux as Windows checksum and size don't output correctly.**

To perform a new release we now must also update the config files and upload the fx SNAPSHOT jar. First thing before the above building steps is to update the version number. This can be found in the poms. For example if we are going to bump from version 1.0.0 to 1.0.1 search and replace all `1.0.0-SNAPSHOT` with `1.0.1-SNAPSHOT`. *Note: UpdateWalletConfig will also need an updated url for the compiled fx-1.0.1-SNAPSHOT.jar which we will add later.*

*Note: if anything was changed with the config project you will need to rebuild this first.*

```
cd config
mvn clean install
```


Once the version number has been updated you can run the build process.

```
mvn clean install
```

In the next step we must update the `fxJarUrl` in UpdateWalletConfig.java. To generate this new url we will have to create a release in this repo [unigrid-update](https://github.com/unigrid-project/unigrid-update/releases). Create a new tag using this format `v1.0.1` then name the release title the version number `1.0.1`.

Now upload the `fx-1.0.1-SNAPSHOT.jar` located in `fx\target` and publish the release. We can now get the url to this jar which should be in this format based on the version number created `https://github.com/unigrid-project/unigrid-update/releases/download/v1.0.1/fx-1.0.1-SNAPSHOT.jar`

Copy this url and add this to the `UpdateWalletConfig.java` file as the `fxJarUrl`

This java file can generate the needed config files for each OS needed for a wallet update. Before we run the file we first need to download each of the daemons into your local computers `Downloads` folder. 

All daemons can be downloaded from [here](https://github.com/unigrid-project/daemon/releases).

*Note: The script looks inside your users/Downloads folder so location is important. If there is an update to the daemon version you will also need to update the file names accordingly.*

At time of these docs the current release of each daemon is as follows. Update accordingly if there is a version update.

```
String linuxDaemon = "unigrid-2.0.2-x86_64-linux-gnu.tar.gz";
String windowsDaemon = "unigrid-2.0.2-win64.zip";
String osxDaemon = "unigrid-2.0.2-osx64.tar.gz";
```

Once you have downloaded the ALL daemons for each OS and updated the versions you can run UpdateWalletConfig. The simplest way to do this is in netbeans. After completion and without errors the output configs will be placed in `config\UpdateWalletConfig`.

Next we need to add these to the repo where we uploaded the `fx-1.0.1-SNAPSHOT.jar` [here](https://github.com/unigrid-project/unigrid-update/).

Be sure that everything else has been uploaded first before updating these configs in the repo. Any wallet checking for updates will look here so all files must be in place already. The best way to do this at the same time is checkout the project and push them at the same time.

```
git clone https://github.com/unigrid-project/unigrid-update.git
```

Then update each config file with each updated output inside `config\UpdateWalletConfig` and push your changes. GitHub can take a few minutes to update the url's and as soon as these update all wallets will have access to the latest changes.

Testing
---------------
To test the bootstrap with different daemon and fx you can pass in different args depending on what you are testing.

To change the config URL FX checks:
```
URL=<CONFIG URL>
```

To change the location FX checks for the bootstrap:
```
BootstrapURL=<DIFFERENT BOOTSTRAP VERSION>
```

Example using a different config file for FX on Linux:
```
cd desktop/target/dist/Unigrid/bin/
./Unigrid URL=https://raw.githubusercontent.com/unigrid-project/unigrid-update-testing/main/config-linux-test.xml
```

Troubleshooting
---------------
If you are running into issues starting the wallet a good place to look is our [documentation](https://docs.unigrid.org/) page.


Automated Testing
-----------------
Developers are strongly encouraged to write unit tests for new code, and to submit new unit tests for old code.
