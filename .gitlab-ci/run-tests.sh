#!/bin/bash

set -e
function cleanup {
    echo -e "\n\n`date '+%Y-%m-%d %H:%M:%S'`: run-tests.sh exiting\n\n"
}
trap cleanup EXIT

echo -e "\n\n`date '+%Y-%m-%d %H:%M:%S'`: run-tests.sh starting\n\n"

# to run tests against a cloud environment, you need to:
# 1. create an API token
# 2. set the CONFORMANCE_TOKEN environment variable to the token
# 3. do "source demo-environ.sh" (or whichever environment you want to run against)
# (to run against your local deployment, just don't do the 'source' command)

source node-client-setup.sh
export TEST_CONFIG_ALIAS='test/a/fintech-clienttest/'
export ACCOUNTS='test/a/fintech-clienttest/open-banking/v1.1/accounts'
export ACCOUNT_REQUEST='test/a/fintech-clienttest/open-banking/v1.1/account-requests'

TESTS=""
EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-server.json|../conformance-suite/.gitlab-ci/expected-failures-ciba.json|../conformance-suite/.gitlab-ci/expected-failures-client.json"
EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-server.json|../conformance-suite/.gitlab-ci/expected-skips-ciba.json|../conformance-suite/.gitlab-ci/expected-skips-client.json"

function makeClientTest {
    # client FAPI-RW-ID2
    TESTS="${TESTS} fapi-rw-id2-client-test-plan:private_key_jwt automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-client-test-plan:mtls automated-ob-client-test.json"

    # client FAPI-RW-ID2-OB
    TESTS="${TESTS} fapi-rw-id2-client-test-plan:openbankinguk-private_key_jwt automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-client-test-plan:openbankinguk-mtls automated-ob-client-test.json"
}

function makeServerTest {
    # authlete openbanking
    TESTS="${TESTS} fapi-rw-id2-test-plan:openbankinguk-mtls authlete-fapi-rw-id2-ob-mtls.json"
    TESTS="${TESTS} fapi-rw-id2-test-plan:openbankinguk-private_key_jwt authlete-fapi-rw-id2-ob-privatekey.json"

    # authlete FAPI
    TESTS="${TESTS} fapi-rw-id2-test-plan:mtls authlete-fapi-rw-id2-mtls.json"
    TESTS="${TESTS} fapi-rw-id2-test-plan:private_key_jwt authlete-fapi-rw-id2-privatekey.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-mtls-test-plan authlete-fapi-r-mtls.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-private-key-test-plan authlete-fapi-r-private-key.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-client-secret-jwt-test-plan authlete-fapi-r-client-secret.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-pkce-test-plan authlete-fapi-r-pkce.json"

    # This is the configuration used in the instructions as an example.
    # We keep it here as we want to be sure code changes don't break the example in the instructions, but the downside is there
    # is a chance that users may be using the alias at the same time our tests are running
    TESTS="${TESTS} fapi-rw-id2-test-plan:private_key_jwt authlete-fapi-rw-id2-privatekey-for-instructions.json"
}

function makeCIBATest {
    # ciba
    TESTS="${TESTS} fapi-ciba-id1-test-plan:poll-mtls authlete-fapi-ciba-id1-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan:poll-private_key_jwt authlete-fapi-ciba-id1-privatekey-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan:openbankinguk-poll-mtls authlete-fapi-ciba-id1-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan:openbankinguk-poll-private_key_jwt authlete-fapi-ciba-id1-privatekey-poll.json"

    # only one backchannel notification endpoint is allowed in CIBA so DCR must be used for ping testing
    # see https://gitlab.com/openid/conformance-suite/issues/389
    TESTS="${TESTS} fapi-ciba-id1-test-plan:ping-mtls authlete-fapi-ciba-id1-mtls-ping-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan:ping-private_key_jwt authlete-fapi-ciba-id1-privatekey-ping-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan:openbankinguk-ping-mtls authlete-fapi-ciba-id1-mtls-ping-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan:openbankinguk-ping-private_key_jwt authlete-fapi-ciba-id1-privatekey-ping-dcr.json"
    # push isn't allowed in FAPI-CIBA profile
    #TESTS="${TESTS} fapi-ciba-id1-push-with-mtls-test-plan authlete-fapi-ciba-id1-mtls-push.json"
}

if [ "$#" -eq 0 ]; then
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules all"
    echo "Run all tests"
    makeServerTest
    makeCIBATest
    makeClientTest
elif [[ ("$#" -eq 1 ) &&  ("$1" = "--client-tests-only" ) ]]; then
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-client.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-client.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules client"
    echo "Run client tests"
    makeClientTest
elif [[ ("$#" -eq 1) && ("$1" = "--server-tests-only") ]]; then
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-server.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-server.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules server"
    echo "Run server tests"
    makeServerTest
elif [[ ("$#" -eq 1) && ("$1" = "--ciba-tests-only") ]]; then
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-ciba.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-ciba.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules ciba"
    echo "Run ciba tests"
    makeCIBATest
else
    echo "Syntax: run-tests.sh [--client-tests-only|--server-tests-only|--ciba-tests-only]"
    exit 1
fi

../conformance-suite/scripts/run-test-plan.py $TESTS
