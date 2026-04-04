# JUnit GameTest Runner

`GameTestRunnerTests` is a JUnit 5 dynamic test bridge that launches existing Loom GameTest tasks.

Each JUnit dynamic test represents one shared game-test function/spec. The runner fans out to the matching Gradle game-test invocations for that function.

## Default behavior

- Disabled by default.
- When enabled, runs this matrix:
  - `fabric:server`
  - `fabric:client`
  - `neoforge:server`

## JVM properties

- `archie.junit.gametest=true` enables execution.
- `archie.junit.gametest.matrix=fabric:server,fabric:client,neoforge:server,neoforge:client` custom matrix.
- `archie.junit.gametest.timeoutMinutes=30` per-invocation timeout.
- `archie.junit.gametest.root=/abs/path/to/Archie` explicit workspace root.
- `archie.junit.gametest.extraArgs=--stacktrace` extra Gradle args.

## IntelliJ run configuration (JUnit)

Use class `net.kernelpanicsoft.archie.gametest.runner.GameTestRunnerTests` with VM options:

`-Darchie.junit.gametest=true -Darchie.junit.gametest.matrix=fabric:server`

## Dedicated Gradle task

Run the preconfigured task:

```bash
./gradlew :common:junitGameTest
```

It enables the runner and points it at the Archie workspace root automatically.


