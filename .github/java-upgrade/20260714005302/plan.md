# Java Upgrade Plan

SESSION_ID: 20260714005302
PROJECT_NAME: password-manager
CURRENT_BRANCH: master
CURRENT_COMMIT_ID: N/A
DATE: 2026-07-14T00:53:02Z

**Guidelines**:
- Upgrade Java runtime to the latest LTS (user request).
- Minimize code changes; prefer build-tool and dependency updates to restore compilation/tests.
- Run tests before and after the upgrade.

**Available Tools**:
- Discovered JDKs: Java 21.0.10 at `/nix/store/.../openjdk-21.0.10+7/lib/openjdk/bin` (available)
- System Maven: Maven 3.9.11 at `/nix/store/.../maven-3.9.11/bin` (available)
- Project Maven wrapper: `./.mvn/wrapper/maven-wrapper.properties` -> Apache Maven 3.9.16 (current)

**Derived Upgrades (summary)**:
- Target Java: 25 (latest LTS)
- Required: Install JDK 25 on the machine (marked `<TO_BE_INSTALLED>`).
- Required: Upgrade Maven wrapper to Maven 4.x (wrapper properties update) — Maven 3.9.x may not support Java 25.
- Project `pom.xml` currently sets `<java.version>21` (will be updated to `25`).

**Technology Stack**:
- Spring Boot parent: `4.1.0` (pom.xml)
- java.version: `21` (pom.xml)
- Build tool: Maven (wrapper present)
- Test framework: Spring Boot Starter Test (maven)

**Key Challenges**:
- Maven wrapper (3.9.16) may not be compatible with Java 25 — will upgrade wrapper to Maven 4.x.
- JDK 25 must be available on the build environment; installing it may require sudo or SDK manager.
- Some dependencies or plugins may require updates if they rely on internal/removed APIs in Java 25.

**Upgrade Steps**:
1. Setup Environment
   - Install JDK 25 on the machine (or make it available) — mark as `<TO_BE_INSTALLED>`.
   - Ensure Maven 4.x is available or plan to upgrade the project Maven wrapper to 4.x.
   - Report `environmentSetup` event with `jdkPath` and `buildToolPath`.

2. Setup Baseline
   - Run `./mvnw -v` and `./mvnw -DskipTests clean test-compile` with current JDK to capture baseline.
   - Record current test pass rate.

3. Upgrade Maven Wrapper
   - Update `.mvn/wrapper/maven-wrapper.properties` to point to Maven 4.x distribution.
   - Run `./mvnw -v` to verify.

4. Update Project Java Version
   - Update `pom.xml` property `<java.version>` from `21` to `25`.
   - Ensure `maven-compiler-plugin` (if configured) uses appropriate `release`/`source`/`target` or add plugin config if absent.

5. Compile & Fix
   - Run `./mvnw -DskipTests clean test-compile` and fix compile errors in both main and test code as required.

6. Full Test Run (Final Validation)
   - Run `./mvnw clean test` and ensure tests pass at 100% (or match/exceed baseline). Fix any failing tests.

7. Summarize & CVE Scan
   - Run dependency enumeration and call CVE validation.
   - Produce `summary.md` with details and next steps.

**Options**:
- Working branch: appmod/java-upgrade-20260714005302
- Run tests before and after the upgrade: true

**Notes / Constraints**:
- VCS detected and changes stashed prior to plan generation.
- System currently has JDK 21 installed; JDK 25 is not present and will be required.
- Maven wrapper currently 3.9.16 — will be upgraded to Maven 4.x as part of Step 3.


---

Plan generated automatically by the upgrade agent. Confirm to proceed.
