# Testing and release automation

## Local test layers

Backend unit, controller, repository and full-context API tests:

```bash
mvn test
```

Android query, parser, repository, ViewModel, permission-policy and state tests:

```bash
cd android-app
./gradlew test lint assembleDebug
```

The instrumented navigation test requires a running emulator:

```bash
cd android-app
./gradlew connectedDebugAndroidTest
```

The end-to-end contract starts MongoDB and the backend, loads two known places,
checks recommendation order and explanations, and passes the real response to
the Android JSON parser:

```bash
./scripts/run-end-to-end.sh
```

## Continuous integration

`.github/workflows/ci.yml` runs backend, Android and end-to-end tests, scans
dependencies, and publishes the backend JAR and debug APK as workflow artifacts
on pushes and pull requests.

`.github/workflows/release.yml` is manual and targets the protected
`production` GitHub Environment. Configure required reviewers on that
environment before using it. An approved run publishes a versioned backend
container and builds the release APK; a hosting provider can deploy that
immutable container without building source in production.
