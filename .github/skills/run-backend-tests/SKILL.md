---
name: run-backend-tests
description: Build and test the admin backend locally (Gradle, two Testcontainers databases, testSetup.sh, exiftool, GitHub Packages credentials, local library builds). Use before reporting any change as done.
---

# Run the vempain-admin-backend tests

- Requirements: Java 25, Docker (two PostgreSQL Testcontainers per ITC/CTC context), `exiftool`, GitHub Packages credentials (`gpr.user`/`gpr.token` or
  `GITHUB_ACTOR`/`GITHUB_TOKEN`) for `vempain-auth-*` and `vempain-common-*`.
- Integration tests need the local environment created once as root: `testSetup.sh --developer-name <login>` (users, SSH keys,
  directories under `/var/tmp`); `testCleanup.sh` removes it. The tests set `spring.profiles.default=test`, so the production TLS
  datasource check is skipped.
- Commands: `./gradlew :service:compileJava :service:compileTestJava`; one class: `./gradlew :service:test --tests '*PublishServiceUTC'`;
  all: `./gradlew clean test` (about four minutes; run one repository's suite at a time).
- Local run: `./docker_db.sh` (Postgres on 5433 and 5434) then `./start.sh` (ports 9090/9091).
- Results in `service/build/test-results/test/*.xml` and `service/build/reports/tests/test/index.html`; a 500 in a CTC is explained in
  the XML `system-out`.
- Unpublished shared library (`vempain-common`, `vempain-auth`): publish it to `~/.m2` from its repo
  (`./gradlew -PreleaseVersion=<version> publishToMavenLocal -x test`), bump `gradle/libs.versions.toml`, and run Gradle with an init
  script that puts `mavenLocal()` first in `allprojects.repositories` (`-I local-maven.init.gradle`).
- Suffixes: `UTC` unit, `CTC` controller, `ITC` integration, `JTC` JSON contract (`api/`). Report tests run and failures from the output.
