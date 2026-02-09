# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the OpenID Foundation conformance suite - a Spring Boot application that validates implementations of OpenID Connect, FAPI1, FAPI2, FAPI-CIBA, OpenID for Identity Assurance (eKYC), Verifiable Credentials (VCI), and Verifiable Presentations (VP).

## Build and Development Commands

```bash
# Build (skip tests)
mvn -B -Dmaven.test.skip -Dpmd.skip clean package

# Run all tests (includes PMD and checkstyle checks)
mvn test

# Run a single test class
mvn test -Dtest=ClassName_UnitTest

# Run tests matching a pattern
mvn test -Dtest="*FAPI*_UnitTest"

# Skip PMD during test
mvn test -Dpmd.skip

# Skip checkstyle during test
mvn test -Dcheckstyle.skip

# Build final JAR
mvn package
```

The built JAR is `target/fapi-test-suite.jar`.

## Running Locally

The conformance suite requires MongoDB and an HTTPS reverse proxy. Use one of:

1. **devenv (Nix-based)**: `devenv up` - Sets up MongoDB, Nginx, TLS certs, and host aliases
2. **Docker Compose**: `docker-compose -f docker-compose-dev.yml up`

Then run the Spring Boot application from your IDE with profile `dev` or:
```bash
java -jar target/fapi-test-suite.jar --spring.profiles.active=dev
```

The app runs at `https://localhost.emobix.co.uk:8443` (regular) and `:8444` (mTLS).

## Architecture

### Test Module System

Tests are organized as **Test Modules** that extend `AbstractTestModule`. Each module:
- Is annotated with `@PublishTestModule` for discovery
- Composes reusable **Conditions** (single validation units)
- Uses an **Environment** object for state management
- Can be parameterized via **Variants** to generate multiple test configurations

```
AbstractTestModule
└── Protocol-specific base class (e.g., AbstractFAPI2SPFinalClientTest)
    └── Concrete test class
```

### Key Base Classes

- `AbstractTestModule` (`testmodule/`) - Core test lifecycle, condition calling, threading
- `AbstractCondition` (`condition/`) - Base for validation units with environment access and logging
- `ConditionSequence` (`sequence/`) - Composes conditions into reusable sequences
- `Environment` (`testmodule/`) - JSON object storage with path navigation

### Condition Calling Patterns

```java
// Fail test immediately on condition failure
callAndStopOnFailure(MyCondition.class);

// Log failure but continue execution
callAndContinueOnFailure(MyCondition.class, Condition.ConditionResult.WARNING);

// Skip if required environment values are missing
skipIfMissing(new String[]{"required_key"}, null, Condition.ConditionResult.INFO,
              MyCondition.class, Condition.ConditionResult.FAILURE);

// Call a sequence of conditions
call(sequence(MySequence.class));
```

### Environment Usage

```java
// Store objects
env.putObject("key", jsonObject);
env.putString("key", "value");

// Retrieve with path navigation
String value = env.getString("object", "nested.path");
JsonObject obj = getJsonObjectFromEnvironment(env, "object", "path");
```

### Package Organization

- `condition/` - Reusable validation conditions (subdivided by `as/` for server-side, `client/` for client-side)
- `testmodule/` - Core framework classes (TestModule, Environment, AbstractTestModule)
- `sequence/` - Reusable condition sequences
- `variant/` - Variant parameter enums and service
- `fapi1advancedfinal/`, `fapi2spfinal/`, `fapi2spid2/`, `fapiciba/` - FAPI protocol tests
- `openid/` - OpenID Connect tests
- `vci10issuer/`, `vci10wallet/` - Verifiable Credentials tests
- `vp1finalverifier/`, `vp1finalwallet/`, `vpid2*/`, `vpid3*/` - Verifiable Presentations tests
- `runner/` - Test execution and HTTP routing
- `plan/` - Test plan organization

### Variants

Tests use `@VariantParameters` to generate multiple configurations from one class:

```java
@VariantParameters({ClientAuthType.class, FAPIResponseMode.class, ...})
public abstract class AbstractFAPI2SPFinalClientTest extends AbstractTestModule {
    // getVariant(ClientAuthType.class) returns the selected variant
}
```

Use `@VariantNotApplicable` to exclude invalid combinations.

### Test Plans

Test plans group related tests for certification via `@PublishTestPlan`:

```java
@PublishTestPlan(testPlanName = "fapi2-security-profile-final", testModules = {...})
public class FAPI2SPFinalTestPlan implements TestPlan {}
```

## Technical Standards & RFCs

When interpreting RFCs or technical specifications, present multiple defensible interpretations with trade-offs rather than committing to a single answer. Flag areas of ambiguity explicitly.

## Code Quality

- **Checkstyle**: Google Java Style (configured in `.checkstyle.xml`)
- **PMD**: Rules in `.pmd.ruleset.xml`
- **Error Prone**: Enabled at compile time with specific exclusions
- **ArchUnit**: Architecture tests in `src/test/java/net/openid/conformance/archunit/`

Tests compile with `-Werror` so all warnings must be resolved.

## Test Naming Convention

Unit test files follow the pattern `*_UnitTest.java` (e.g., `MyCondition_UnitTest.java`).

## Creating New Tests

1. Create a condition class extending `AbstractCondition` with `@PreEnvironment`/`@PostEnvironment` annotations
2. Create a test module extending the appropriate abstract base class
3. Annotate with `@PublishTestModule`
4. Add to a test plan
