# CLAUDE.md

This file provides guidance to coding agents when working with code in this repository.

## Project Overview

This is the OpenID Foundation conformance suite - a Spring Boot application that validates implementations of OpenID Connect, FAPI1, FAPI2, FAPI-CIBA, OpenID for Identity Assurance (eKYC), Verifiable Credentials (VCI), and Verifiable Presentations (VP).

## Workflow Rules

After making code changes, always run the project build and tests before committing. If tests fail, fix them before presenting the result as complete.

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

# Run ArchUnit tests (quote pattern to avoid shell glob expansion)
mvn test -Dtest='*ArchUnit*'

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

### Kotlin Sources

The project is primarily Java but contains Kotlin source files for multipaz library integration:
- `src/main/java/com/android/identity/testapp/` - VP test credential provisioning (TestAppUtils.kt)
- `src/main/java/org/multipaz/testapp/` - VCI mdoc credential creation (VciMdocUtils.kt)

These use the [multipaz](https://github.com/openwallet-foundation/multipaz) library for mdoc/SD-JWT credential operations.

### JSON Schema Validation

Spec compliance checks can be implemented using JSON Schema validation. Schemas live in `src/main/resources/json-schemas/` and conditions extend `AbstractJsonSchemaBasedValidation`:

```java
public class ValidateDCQLQuery extends AbstractJsonSchemaBasedValidation {
    @Override
    protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
        JsonObject dcql = (JsonObject) env.getElementFromObject("client", "dcql");
        return new JsonSchemaValidationInput("DCQL query",
            "json-schemas/oid4vp/dcql_request.json", dcql);
    }
}
```
- Keep validation strict where the specification defines fixed fields.
- Unknown properties should raise warnings (not errors)
- Warning vs error severity is chosen by the caller of a condition; if unknown-property findings must be warnings while schema violations remain errors, use a separate condition path and call it with warning severity.

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

Key specifications for VP/VCI work:
- **OID4VP 1.0 Final**: https://openid.net/specs/openid-4-verifiable-presentations-1_0.html
- **OID4VP WG Draft**: https://openid.github.io/OpenID4VP/openid-4-verifiable-presentations-wg-draft.html
- **OID4VP GitHub**: https://github.com/openid/OpenID4VP

Identity Assurance spec locations used in this codebase:
- https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html
- https://openid.net/specs/openid-connect-4-ida-attachments-1_0.html
- https://openid.net/specs/openid-connect-4-ida-claims-1_0.html
- https://openid.net/specs/openid-ida-verified-claims-1_0.html

## Test-Suite Behavior Expectations

- This repository is a conformance test suite; explicit failures for invalid protocol behavior are expected.
- Ignored catches can be acceptable if they still lead to a clear and meaningful test failure.
- Generic `error(...)` text is acceptable when `args(...)` includes actionable detail.

### Sender vs Receiver Validation

When specs say "MUST ignore unknown properties", that applies to **receivers** (e.g., wallets processing DCQL queries). The conformance suite validates **senders** (e.g., verifiers constructing DCQL queries), so JSON schemas SHOULD use `additionalProperties: false` to flag unknown or misspelled fields as warnings (not errors) — senders should not include undefined properties.

## Code Quality

- **Checkstyle**: Google Java Style (configured in `.checkstyle.xml`)
- **PMD**: Rules in `.pmd.ruleset.xml`
- **Error Prone**: Enabled at compile time with specific exclusions
- **ArchUnit**: Architecture tests in `src/test/java/net/openid/conformance/archunit/`
- **JSON access in Java**: Avoid `JsonElement.getAsString/getAsInt/getAsLong/...`; use `OIDFJSON` helpers instead (e.g., `OIDFJSON.getString(...)`) to satisfy ArchUnit and avoid implicit conversions.

Tests compile with `-Werror` so all warnings must be resolved.

## Code Review

When asked to review a commit or branch, structure the review by file and call out: correctness issues (especially dead code or unreachable paths), API misuse, and behavioral changes. Don't just summarize — actively look for bugs.

## Git Workflow Preference

- For history edits, do not use `git -c ...` or environment-wrapped rebase commands.
- Prefer plain `git` commands (including multi-step non-interactive flows) to avoid extra permission prompts.

## Git Operations

When making multi-file changes or library upgrades, create separate atomic commits per logical change. Before committing, verify the build passes for each commit independently.

## Test Naming Convention

Unit test files follow the pattern `*_UnitTest.java` (e.g., `MyCondition_UnitTest.java`).

## Key Dependencies

- **multipaz** (CBOR/COSE/mdoc library): Source is at https://github.com/openwallet-foundation/multipaz — use this to look up API details rather than unpacking JARs.
- **Nimbus JOSE+JWT** (JWT/JWK/JWS/JWE library): Source is at https://bitbucket.org/connect2id/nimbus-jose-jwt/src/master/ — use this to look up API details rather than unpacking JARs.

## Creating New Tests

1. Create a condition class extending `AbstractCondition` with `@PreEnvironment`/`@PostEnvironment` annotations
2. Create a test module extending the appropriate abstract base class
3. Annotate with `@PublishTestModule`
4. Add to a test plan
