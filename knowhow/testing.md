# Unit tests

- Run: `cd LarvaSecondsLostExtMod && ant TEST` (compiles `src/` via `COMPILE`, then the tests via `COMPILE_TESTS`, then runs every `*Test` class with `org.junit.runner.JUnitCore`; any failure fails the build). No Scelight runtime needed.
- Sources: `LarvaSecondsLostExtMod/test/hu/aleks/larvasecondslostextmod/` (same package as production, so package-private members are reachable). Class names must end in `Test`.
- Jars: JUnit 4.13.2 + hamcrest-core 1.3 in `LarvaSecondsLostExtMod/test-lib/`. **Never put test jars in `lib/`**: `COMPILE` uses `lib/**/*.jar` and `BUILD_RELEASE` copies it into the release.
- Output: test classes go to `bin-test/` (removed by `CLEAN`). Tests compile with `source/target 1.7`, so lambdas etc. are rejected, like in production code. The build uses whatever JDK `ant` finds (JDK 17 here, with `-source 7`); Java 8+ API calls are not caught by the compiler, avoid them manually.
- `BUILD_RELEASE` does not depend on `TEST`. Release routine: `ant TEST BUILD_RELEASE`. The release jar, release folder and deployment zip contain no test classes or JUnit/hamcrest jars (check with `unzip -l`).
- Tests use synthetic data only. Anything that parses replays is a golden/integration/manual check.
