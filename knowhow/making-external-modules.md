External Modules
================

# Introduction
----------------------------------

External Modules are software components that can be integrated into and run along with Scelight, but they are developed and maintained independently from Scelight by 3rd party developers and vendors.

External Modules can be great assets to make your Scelight and StarCraft II experience better.

Since External Modules are NOT developed or maintained by me, you should have the same reservations about them as about any applications you download from the internet.

ONLY INSTALL EXTERNAL MODULES THAT COME FROM TRUSTED SOURCES!

Since external modules are NOT developed by me, I take zero responsibility for what they do or how they do it. (This also applies to Official external modules.) Poorly or viciously written modules can make your Scelight or your whole system slow, unstable, or can even damage it. External modules can also access and steal private data from your computer!

AGAIN: ONLY INSTALL EXTERNAL MODULES THAT COME FROM TRUSTED SOURCES!

### Official External Modules

Official External Modules are general purpose external modules from cooperating 3rd party developers and vendors. Official External Modules are listed inside Scelight on the Available Modules page, and they can be installed with one click, and have auto-updated. When an official external module is installed, it is disabled by default. You have to manually enable it on the Installed Modules page.

Note: When you disable auto-update for an installed official external module, the module will remain installed in your Scelight and you can keep using it (only it will not be auto-updated until you re-enable auto-update for it). To permanently uninstall / remove a module, you also have to go to the Installed Modules page and delete / uninstall it.

Since Official external modules are also NOT developed by me, the same policy applies to them too (I take zero responsibility for damages caused by them).

ONLY INSTALL OFFICIAL EXTERNAL MODULES THAT COME FROM TRUSTED AUTHORS!

### Installed External Modules

Installed external modules are external modules that are already downloaded and present in your Scelight and are ready to be used.

Before you can use them, you have to manually enable them. Disabled external modules are never loaded and started.

External modules can be installed either automatically by enabling them on the Available Modules page or manually by downloading them from the internet and extracting them in the mod-x folder inside the Scelight folder.

Note: If you delete / uninstall an official external module, you also have to disable auto-update for it on the Available Modules page or else it will be installed again automatically on the next startup.

ONLY INSTALL AND ENABLE EXTERNAL MODULES THAT COME FROM TRUSTED SOURCES!

# External Module Development
-------------------------------------------------

_The following information is intended for developers._

External Modules are written in the Java language, just like Scelight itself. There are no restrictions, everything can be used from the public Java API. Since the Java requirement of Scelight is Java 7.0, external modules can assume the availability of the standard Java 7.0 library. For the sake of compatibility and portability it is highly recommended to use only the Java 7.0 API (and not newer versions).

You can download the External Module SDK from the [Downloads](/site/scelight/downloads) page. The SDK is a complete [Eclipse](https://www.google.com/url?q=https%3A%2F%2Fwww.eclipse.org%2F&sa=D&sntz=1&usg=AOvVaw0_CvvQ96rz1hkIZeBitRdX) project with an [Ant](http://www.google.com/url?q=http%3A%2F%2Fant.apache.org%2F&sa=D&sntz=1&usg=AOvVaw3dhW9SxdNqPkw7rWXrJLdN) build script which can build releases of the external module, make deployments, install / inject it into a Scelight installation and make project backups. The SDK also contains the External Module API library and its Javadoc.

To get started with the SDK, first download it and extract it, then use the File / Import... menu in Eclipse, choose "Existing Projects into Workspace" and select the extracted ScelightExtModSDK folder which is the Eclipse project root.

There is a directory-info.html file at the root of the Eclipse project which explains the folder structure of the SDK Eclipse project. Check that out for a start.

**The Ant script (build.xml) provides the following tasks:**

1.  BUILD\_RELEASE: Creates a release and deployment of the external module.
2.  INSTALL\_DEPLOYMENT: Installs the deployment into the Scelight folder. Also deletes previously installed versions of the external module.
3.  BACKUP\_PROJECT: Creates a project backup. Zips the files of the project (excluding the bin folder).

**The Ant script uses properties to perform tasks which you can specify in the following files inside the Eclipse project:**

*   release/release.properties
*   release/resources/Scelight-mod-x-manifest-template.xml: This is a template, parameters will be replaced by the Ant BUILD\_RELEASE task.
*   release/resources/module-template.xml: This is a template, parameters will be replaced by the Ant BUILD\_RELEASE task.

The included External Module API library also contains the source code of the interface. This means whenever you use the auto-complete feature of Eclipse or you move your mouse cursor over different parts of your code, the Javadoc of the External Module API will be displayed to you.

The External Module SDK also contains an example Hello World external module with source code. The SDK supports developing one External Module. If you want to develop multiple External Modules, the easiest way to do this with the SDK if you make copies of the SDK folder (and rename them).

The External Module API Javadoc is also a number one source of information, and it is available online for browsing here: [Scelight External Module API Javadoc](https://scelightop.appspot.com/scelight-ext-mod-api/).

Note: If you want to use a different IDE other than Eclipse, you have to create and set it up yourself. I only provide support for Eclipse. Of course you can use the External Module API and the provided Ant script in your own projects.

The Scelight External Module SDK is not a requirement to develop External Modules but it is a convenience. You can develop External Modules without the Scelight External Module SDK too, the only thing that is required is the External Module API library which is located under the Scelight-ext-mod-api folder inside the ScelightExtModSDK project folder.

**The main exposed components of the External Module API:**

*   Application logger.
*   Language and locale specific utilities.
*   Extended and custom swing GUI component library.
*   Settings. External modules can specify settings and integrate into the Settings dialog; take advantage of setting dependent and bound components and listen to setting changes.
*   Replay parser engine.
*   Replay processor engine.
*   Basic SC2 Balance Data.
*   Name template engine.
*   Replay search engine.
*   SC2 monitor (current game status, live APM, game status change listening).
*   Replay folder monitor (new replay listening).
*   Sound utilities (background sound player - including MP3 format).
*   General utilities to help implement any kind of code.

More will be added / exposed in the future.

### External Module files and folder structure

An External Module is a set of files in a well-defined (and mandatory) folder structure.

All external modules must be placed in the mod-x folder inside the Scelight folder. Each external module must have its own folder. An external module is identified by its folder! It is recommended not to use capital letters, spaces and other special characters in folder names. Dashes should be used instead of spaces. This is a good example: "hello-world".

In the folder of the external module a version sub-folder must exist, named after the version of the external module. The version may have 3 parts at the most in the format of "major.minor.revision", each part being a non-negative integer number. It is recommended to leave out the revision part if it is 0, but it is recommended to include the minor part even if it is 0, e.g. in case of "1.0.0" the version folder must be named "1.0".

All files of the external modules must be put inside the version folder, but the files of the external module may be grouped arbitrarily in sub-folders in any depth, but they all must be in the version folder. If a newer version is created and released for an external module, the external module folder must not be changed but the version sub-folder inside it must obviously be changed. If a modified variation of an external module is published, its version must be changed (incremented). Generally speaking 2 instances of the same external module must only have the same version if and only if their content (all the files of the external module and their structure) is exactly the same.

The version folder must contain a file called the External Module manifest. It must be named "Scelight-mod-x-manifest.xml". This is an XML descriptor of the metadata of the External Module.

The version folder might contain any additional files and Java libraries (jar files) the external module needs. All jar files (\*.jar) detected in the version folder recursively will be added to the external module's class path and be made available automatically when an external module is loaded and started. The code of the external module (the compiled Java class files) must also be packaged into jars and placed in the version folder (optionally in sub-folders of the version folder). The external module API library must not be placed here, it is integrated into Scelight itself. The external module API library is only required for the development stage but not for the deployment stage.

Example External Module structure:

\-"Scelight"

\-"mod-x"

\-"hello-world"

\-"1.0"

\-"Scelight-mod-x-manifest.xml"

\-"hello-world.jar"

### External Module Manifest

As mentioned above, the External Module Manifest is an XML file named "Scelight-mod-x-manifest.xml" containing metadata about the External Module.

**The following info can / must be provided in the manifest file:**

*   <name>: \[Required\] Display name of the external module.
*   <version>: \[Required\] Version of the external module. Version parts are specified as attributes: major, minor, revision. This must match the name of the version folder.
*   <buildInfo>: \[Required\] Build info of the actual external module release.
    *   <buildNumber>: \[Required\] Build number of the actual release. This must be incremented each time a new release is published. It is recommended to increment on each internal build during the development process.
    *   <date>: \[Required\] Release date and time of the actual version of the external module. Required format is "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" as described in XML Schema Part 2: Datatypes for xsd:dateTime.
*   <apiVersion>: \[Required\] The used External Module API version. This should be the External Module API version that was used to develop the external module. See the Javadoc of IExtModManifestBean for more details.
*   <folder>: \[Required\] Folder identifier of the external module. This must match the folder name the external module is in (the parent of the version folder).
*   <iconImgData>: \[Recommended\] Base64 encoded icon of the external module, image data in one of the formats of JPG, PNG or GIF, in size of 16x16.
*   <authorList>: \[Required\] Author of the external module. This element can be added multiple times if multiple authors are to be listed.
    *   <personName>: \[Required\] Name of the author.
        *   <first>: First name of the author.
        *   <middle>: Middle name of the author.
        *   <last>: Last name of the author.
        *   <nick>: Nickname of the author.
    *   <contact>: \[Recommended\] Contact details of the author.
        *   <location>: The geographical location of the author (e.g. City and Country).
        *   <email>: \[Recommended\] Email address of the author.
        *   <facebook>: Facebook address of the author.
        *   <googlePlus>: Google+ address of the author.
        *   <twitter>: Twitter address of the author.
        *   <linkedIn>: LinkedIn address of the author.
        *   <youtube>: YouTube address of the author.
        *   <other>: Other address of the author.
*   <homePage>: \[Recommended\] Home page address of the external module.
*   <shortDesc>: \[Recommended\] Short, 1-line description of the external module. Must be in plain text format.
*   <description>: \[Recommended\] Long HTML description of the external module. HTML support: [HTML 3.2](http://www.google.com/url?q=http%3A%2F%2Fwww.w3.org%2FTR%2FREC-html32.html&sa=D&sntz=1&usg=AOvVaw12SLugLMwUgSA4GvWDRRb1) with no scripts allowed (neither embedded nor referenced).
*   <mainClass>: \[Required\] The fully qualified name of the main class (entry point) of the external module. Must be unique. Must implement the IExternalModule interface and must have a no-arg constructor.

Here is an example manifest:

Example External Module Manifest XML:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<extModManifestBean v="1">

<name>Hello World</name>

<version major="1" minor="1" revision="0" v="1"/>

<buildInfo v="1">

<buildNumber>106</buildNumber>

<date>2014-03-07T09:14:53.809+01:00</date>

</buildInfo>

<apiVersion major="1" minor="1" revision="0" v="1"/>

<folder>hello-world</folder>

<iconImgData>iVBORw0KGgoAAAANS...</iconImgData> <!-- Trimmed for display -->

<authorList v="1">

<personName v="1">

<first>András</first>

<last>Belicza</last>

</personName>

<contact v="1">

<email>someemail@gmail.com</email>

</contact>

</authorList>

<homePage>https://sites.google.com/site/scelight/</homePage>

<shortDesc>Hello World example</shortDesc>

<description><!\[CDATA\[

<html><body>

<p>This is a simple Hello World example external module showcasing the basic features of the API.</p>

<p>Source code is available on the <a href="https://sites.google.com/site/scelight/downloads">Scelight Downloads</a> page inside the Scelight External Module SDK archive.</p>

</body></html>

\]\]></description>

<mainClass>hu.belicza.andras.helloworldextmod.HelloWorld</mainClass>

</extModManifestBean>
```

The loaded manifest is passed on to the external module it its init() method as an IExtModManifestBean instance, and is also made available via the module environment (IModEnv.getManifest()).

### Official External Modules development

Official External Modules are normal External Modules. The only difference is that Official External Modules are listed inside the application, users can list them and install them with one click, and have them auto-updated, while non-official external modules have to be downloaded and installed manually.

A central list of Official External Modules is managed by the Scelight Operator. When the Scelight Launcher checks for updates, it also retrieves the list of Official External Modules, and also checks whether there are newer versions available of the enabled official external modules. If there are, they will be dowloaded and installed properly by the Scelight Launcher. If not, an integrity check will be performed whether the installed official external modules are modified or corrupted. If any alteration is detected, they will be repaired.

The list of Official External Modules is managed by the Scelight Operator, but the Scelight Operator does not know about the current versions of the Official external modules nor does it not know about their content (e.g. files and their content hashes).

Official External modules must provide an XML descriptor called the module.xml. This module bean contains general info about the module (name and folder) and contains the latest version, the archive URL (downloadable archive of the latest version) and the list of its content (file paths with hashes). The Ant script of the External Module SDK also generates this module.xml file. The Scelight Operator distributes the locations of the module.xml beans of the Official External Modules, and the Scelight Launcher acquires these to perform the install, update and repair operations of the enabled official external modules.

This way the authors of the Official external modules have full control over when and what they release: they (their servers) are responsible to host the module.xml file which describes the latest version and its download URL(s), and also their servers serve the downloads. Whenever the authors want to publish a new version, they just have to make the new module.xml file available (under the same URL).

**The following info can / must be provided in the module.xml file:**

*   <name>: \[Required\] Display name of the external module. This must match the name used in the manifest.
*   <version>: \[Required\] Version of the external module. This must match the version used in the manifest.
*   <folder>: \[Required\] Folder identifier of the external module. This must match the folder used in the manifest.
*   <archiveFile>: \[Required\] Name of the archive file holding the files of the external module. Must specify the archive file name as path and its content SHA-256 digest as sha256 attributes.
*   <urlList>: \[Required\] URL where the archive file can be downloaded. This element can be added multiple times if there are alternate locations / mirrors.
*   <archiveSize>: \[Required\] Size of the archive file in bytes.
*   <fileList>: \[Required\] Each file in the archive must have a <fileList> element specifying its name with full path as the path attribute and its content SHA-256 digest as the sha256 attribute.

Here is an example module.xml:

Example module.xml:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<moduleBean v="1">

<name>Hello World</name>

<version major="1" minor="1" revision="0" v="1"/>

<folder>hello-world</folder>

<archiveFile path="HelloWorld-1.1.zip" sha256="9cc734b36bc5cc870c5961ccaa29d9d7504a75eec08f1ed664c05b51c6368e0a" v="1"/>

<urlList>http://yoursite.com/ext-mod-path/HelloWorld-1.1.zip</urlList>

<archiveSize>19244</archiveSize>

<fileList path="Scelight/mod-x/hello-world/1.1/Scelight-mod-x-manifest.xml" sha256="73cf804da189412d28aa7ba5f851d7510a0105cb2b33d4cfc22376ca0de506c9" v="1"/>

<fileList path="Scelight/mod-x/hello-world/1.1/hello-world.jar" sha256="78dd17108766653bed04f1f16fe9ae377a16066c4e7ec82d88bce34b82b8d76c" v="1"/>

</moduleBean>
```