gradle-nbm-plugin
=================

A Gradle plugin to build [NetBeans](http://www.netbeans.org/) modules and RCP applications.

The project is not actively developed. 

# Features

## Build standalone NetBeans modules

* Builds a module.
 * Compiles sources.
 * Process manifest by adding deps and required attributes (public packages, friends, version, ...)
 * Create module files layout: module JAR, bundled JAR (from dependencies), update tracking?, help files, localization/branding
* Builds NBM file for the module.
 * Signing can be configured in a build script
* Runs unit tests
* It is possible to run NetBeans with this module installed.

See the [example build script](https://github.com/radimk/gradle-nbm-plugin/blob/master/samples/standalone/build.gradle)
to find how to customize module build.
Things that you need to do in your build script include:

* Apply the plugin. You can follow the directions from [Gradle plugin portal](http://plugins.gradle.org/plugin/cz.kubacki.nbm)
* Add a NetBeans Maven repository or any other source of NetBeans modules.
* Add `nbm` section defining model how the module should be built.
  Until there is a better documentation the available properties can be found in an [extension class](https://github.com/radimk/gradle-nbm-plugin/blob/master/plugin/src/main/groovy/org/gradle/plugins/nbm/NbmPluginExtension.java)
* Optionally you can add an `Exec` task to run NetBeans with this module.
  The sample creates a `netbeans.conf` file where it adds a property specifying location of extra module and runs NetBeans using its own [user directory](http://wiki.netbeans.org/FaqWhatIsUserdir).

NetBeans evangelist Geertjan Wielenga wrote a nice introductory post about [How to build NetBeans modules with Gradle](https://blogs.oracle.com/geertjan/entry/how_to_build_netbeans_modules).

# TODOs

* fix resource lookup when running annotation processors - see [FAQ](https://github.com/radimk/gradle-nbm-plugin/wiki/FAQ)
* possibly generate files from annotations into special directory to simplify merges
* solve AUC generation
* testing
* module dependencies: use new configuration(s) to allow changes
* solve how to run multiple modules together (fileTree copying + netBeansRun)
* autoload/eager modules
* RCP applications

# Goals

## Build standalone NetBeans modules

* Run NetBeans with this module
 * support debugging (optional)

## Build module suite

* same as above for multiple modules
* interdependencies (implementation deps)
* generate autoupdate center files layout

## NetBeans RCP applications

* Build an application
* Run it
* Create an installer
* Run tests

# Development notes

To do a release run
```
./gradlew release -Prelease.scope=minor -Prelease.stage=final
```
