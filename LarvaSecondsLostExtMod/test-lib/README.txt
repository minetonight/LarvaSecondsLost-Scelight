Test-only jars (JUnit). NEVER move these to lib/: COMPILE puts lib/**/*.jar on the classpath
and BUILD_RELEASE copies lib/**/*.jar into the release. Only the TEST targets use this folder.

junit-4.13.2.jar        https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
                        SHA-1 8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12
hamcrest-core-1.3.jar   https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
                        SHA-1 42a25dc3219429f0e5d060061f71acb49bf010a0

The *.jar.sha1 files are the checksums published next to the jars on Maven Central.
Verify: sha1sum -c <(for f in *.jar; do echo "$(cut -d' ' -f1 $f.sha1)  $f"; done)
