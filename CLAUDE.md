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

### Running integration tests

When running integration tests locally, the setup that consistently works is:

```bash
# 0) Build the jar first (especially after code changes)
mvn -DskipTests package

# 1) Stop any existing suite process (ignore if not running)
#    The first command catches jar launches; the second catches IntelliJ/classpath launches.
#    Note: the port-based command kills any process listening on :8080.
pkill -f "target/fapi-test-suite.jar" || true
kill $(lsof -tiTCP:8080 -sTCP:LISTEN) 2>/dev/null || true

# 2) Start suite behind local proxy target, with dev mode enabled
#    Run this in the foreground in a dedicated terminal.
java -jar target/fapi-test-suite.jar \
  --spring.data.mongodb.uri=mongodb://127.0.0.1:27017/test_suite \
  --server.port=8080 \
  --fintechlabs.devmode=true \
  --fintechlabs.base_url=https://localhost.emobix.co.uk:8443 \
  --fintechlabs.base_mtls_url=https://localhost.emobix.co.uk:8444

# 3) (Recommended) confirm proxy + runner API readiness before launching plans
# run this probe outside any sandbox/network-restricted environment
# manual probe waits up to 20s (20 attempts x 1s); run-test-plan.py has similar startup retries
ready=0
for attempt in $(seq 1 20); do
  body="/tmp/runner-available-$$.json"
  code=$(curl -k -sS -o "$body" -w '%{http_code}' https://localhost.emobix.co.uk:8443/api/runner/available || true)
  if [ "$code" = "200" ] && python3 -c 'import json,sys; json.load(open(sys.argv[1]))' "$body" >/dev/null 2>&1; then
    echo "runner/available is ready (attempt $attempt/20)"
    ready=1
    rm -f "$body"
    break
  fi
  echo "runner/available not ready yet (attempt $attempt/20, http=$code)"
  rm -f "$body"
  sleep 1
done
if [ "$ready" -ne 1 ]; then
  echo "runner/available did not become ready after 20s; check proxy stack (devenv/docker)."
  echo "Continuing anyway: run-test-plan.py also retries runner startup."
fi

# 4) Run federation plans (through proxy URL)
cd ../conformance-suite-private
../conformance-suite/.gitlab-ci/run-tests.sh --federation-tests
```

Canonical command form (use these exact commands/ordering to avoid extra permission prompts):

```bash
mvn -DskipTests package
pkill -f "target/fapi-test-suite.jar" || true
# Note: this kills any process listening on :8080.
kill $(lsof -tiTCP:8080 -sTCP:LISTEN) 2>/dev/null || true
java -jar target/fapi-test-suite.jar \
  --spring.data.mongodb.uri=mongodb://127.0.0.1:27017/test_suite \
  --server.port=8080 \
  --fintechlabs.devmode=true \
  --fintechlabs.base_url=https://localhost.emobix.co.uk:8443 \
  --fintechlabs.base_mtls_url=https://localhost.emobix.co.uk:8444
cd ../conformance-suite-private
../conformance-suite/.gitlab-ci/run-tests.sh --federation-tests
```

`--federation-tests` can be changed to run other test suites. Prefer selecting different existing `run-tests.sh` options over editing the script itself.

### Running VP-only tests

There is no dedicated `--vp-tests` option in `.gitlab-ci/run-tests.sh`. To run only VP plans from `--server-tests-only`, temporarily comment out the OID4VCI `TESTS=` lines inside `makeServerTest`, run tests, then restore the file.

```bash
# 1) Backup run-tests.sh
cp .gitlab-ci/run-tests.sh /tmp/run-tests.sh.vp.bak

# 2) Temporarily comment OID4VCI TESTS= lines in makeServerTest
awk 'BEGIN{in_vci=0}
  /# OpenID4VCI op-against-rp/{in_vci=1}
  in_vci && /^[[:space:]]*TESTS="/{print "#" $0; next}
  in_vci && /^}/{in_vci=0}
  {print}
' .gitlab-ci/run-tests.sh > /tmp/run-tests.sh.vp
mv /tmp/run-tests.sh.vp .gitlab-ci/run-tests.sh
chmod +x .gitlab-ci/run-tests.sh

# 3) Run server tests (now VP-only)
cd ../conformance-suite-private
../conformance-suite/.gitlab-ci/run-tests.sh --server-tests-only

# 4) Restore run-tests.sh
cd ../conformance-suite
cp /tmp/run-tests.sh.vp.bak .gitlab-ci/run-tests.sh
chmod +x .gitlab-ci/run-tests.sh
```

Expected signal in output: queued plans should be only `oid4vp-*` (no `oid4vci-*`).

Important details:
- Use the HTTPS proxy endpoint (`https://localhost.emobix.co.uk:8443`), not direct HTTP calls to port `8080`. Direct HTTP gets rejected by `RejectPlainHttpTrafficChannelProcessor`.
- Run the step-3 readiness probe outside sandbox/network-restricted execution so `curl` can actually reach the proxy endpoint.
- `scripts/run-test-plan.py` also has built-in startup retries against `api/runner/available` (11 attempts with 10-second backoff; effective wait is typically ~100-120s including request time), so brief startup races are expected.
- Keep `fintechlabs.devmode=true` for local scripted runs so auth does not block test execution.
- Ensure the `python3` used to run test scripts has `pyparsing >= 3` for `scripts/run-test-plan.py`. On macOS, one way is `PATH=/opt/homebrew/bin:$PATH` before running the script. If you see `'Group' object has no attribute 'set_results_name'`, the wrong Python environment is being used.
- The `cd ../conformance-suite-private` step assumes this checkout layout; if you are already in `conformance-suite`, run `.gitlab-ci/run-tests.sh --federation-tests` directly.
- Run the Java server in a separate terminal, in the foreground, and stop it after tests complete (Ctrl-C).
- If you must run Java in the background, capture logs to a file and verify readiness via `runner/available` before starting plans.

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

OpenID Federation spec locations:
- **OpenID Federation 1.0**: https://openid.net/specs/openid-federation-1_0.html

## Error Messages for Configuration Issues

When a condition fails because of missing or invalid test configuration (fields the user fills in on schedule-test.html), error messages should reference the UI labels the user sees, not internal JSON key names. Include "in the test configuration" so the user knows where to look. For example:

- Good: `"'Payment consent request JSON' field is missing from the 'Resource' section in the test configuration"`
- Bad: `"brazilPaymentConsent not found in resource configuration"`

Check `src/main/resources/static/schedule-test.html` for the field labels displayed to users.

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
