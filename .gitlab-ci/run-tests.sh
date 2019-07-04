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

TESTS="--expected-failures-file ../conformance-suite/.gitlab-ci/expected-failures.json"

function makeClientTest {
    # client FAPI-RW-ID2
    TESTS="${TESTS} fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-test-plan automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-client-test-with-mtls-holder-of-key-test-plan automated-ob-client-test.json"

    # client FAPI-RW-ID2-OB
    TESTS="${TESTS} fapi-rw-id2-ob-client-test-with-private-key-jwt-and-mtls-holder-of-key-test-plan automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-test-plan automated-ob-client-test.json"
}

function makeServerTest {
    # authlete openbanking
    TESTS="${TESTS} fapi-rw-id2-ob-with-mtls-test-plan authlete-fapi-rw-id2-ob-mtls.json"
    TESTS="${TESTS} fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan authlete-fapi-rw-id2-ob-privatekey.json"

    # forgerock currently disabled: not managed to onboard new clients since their period of frequent key recycling
    #TESTS="${TESTS} fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan forgerock-code-idtoken-config.json"
    #TESTS="${TESTS} fapi-rw-id2-ob-code-with-private-key-and-mtls-holder-of-key-test-plan forgerock-code-config.json"

    # ozone openbanking
    #TESTS="${TESTS} fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan ozone-ob-privatekey-codeidtoken-v2_0.json"
    #TESTS="${TESTS} fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan ozone-ob-privatekey-codeidtoken-v3_1.json"

    # authlete FAPI
    TESTS="${TESTS} fapi-rw-id2-with-mtls-test-plan authlete-fapi-rw-id2-mtls.json"
    TESTS="${TESTS} fapi-rw-id2-with-private-key-and-mtls-holder-of-key-test-plan authlete-fapi-rw-id2-privatekey.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-mtls-test-plan authlete-fapi-r-mtls.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-private-key-test-plan authlete-fapi-r-private-key.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-client-secret-jwt-test-plan authlete-fapi-r-client-secret.json"
    TESTS="${TESTS} fapi-r-code-id-token-with-pkce-test-plan authlete-fapi-r-pkce.json"

    # This is the configuration used in the instructions as an example.
    # We keep it here as we want to be sure code changes don't break the example in the instructions, but the downside is there
    # is a chance that users may be using the alias at the same time our tests are running
    TESTS="${TESTS} fapi-rw-id2-with-private-key-and-mtls-holder-of-key-test-plan authlete-fapi-rw-id2-privatekey-for-instructions.json"
}

function makeCIBATest {
    # ciba
    TESTS="${TESTS} fapi-ciba-test-plan:poll-mtls authlete-fapi-ciba-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-test-plan:poll-private_key_jwt authlete-fapi-ciba-privatekey-poll.json"
    TESTS="${TESTS} fapi-ciba-test-plan:openbankinguk-poll-mtls authlete-fapi-ciba-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-test-plan:openbankinguk-poll-private_key_jwt authlete-fapi-ciba-privatekey-poll.json"

    # only one backchannel notification endpoint is allowed in CIBA so DCR must be used for ping testing
    # see https://gitlab.com/openid/conformance-suite/issues/389
    TESTS="${TESTS} fapi-ciba-test-plan:ping-mtls authlete-fapi-ciba-mtls-ping-dcr.json"
    TESTS="${TESTS} fapi-ciba-test-plan:ping-private_key_jwt authlete-fapi-ciba-privatekey-ping-dcr.json"
    TESTS="${TESTS} fapi-ciba-test-plan:openbankinguk-ping-mtls authlete-fapi-ciba-mtls-ping-dcr.json"
    TESTS="${TESTS} fapi-ciba-test-plan:openbankinguk-ping-private_key_jwt authlete-fapi-ciba-privatekey-ping-dcr.json"
    # push isn't allowed in FAPI-CIBA profile
    #TESTS="${TESTS} fapi-ciba-push-with-mtls-test-plan authlete-fapi-ciba-mtls-push.json"
}

if [ "$#" -eq 0 ]; then
    TESTS="${TESTS} --show-untested-test-modules all"
    echo "Run all tests"
    makeServerTest
    makeCIBATest
    makeClientTest
elif [[ ("$#" -eq 1 ) &&  ("$1" = "--client-tests-only" ) ]]; then
    TESTS="${TESTS} --show-untested-test-modules client"
    echo "Run client tests"
    makeClientTest
elif [[ ("$#" -eq 1) && ("$1" = "--server-tests-only") ]]; then
    TESTS="${TESTS} --show-untested-test-modules server"
    echo "Run server tests"
    makeServerTest
elif [[ ("$#" -eq 1) && ("$1" = "--ciba-tests-only") ]]; then
    TESTS="${TESTS} --show-untested-test-modules ciba"
    echo "Run ciba tests"
    makeCIBATest
else
    echo "Syntax: run-tests.sh [--client-tests-only|--server-tests-only|--ciba-tests-only]"
    exit 1
fi

../conformance-suite/scripts/run-test-plan.py $TESTS
