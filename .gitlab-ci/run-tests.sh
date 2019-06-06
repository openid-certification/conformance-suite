#!/bin/bash

set -e
function cleanup {
    if [ -n "${PORT_FORWARD_PID}" ]; then
      echo "Killing kubectl port-forward"
      kill ${PORT_FORWARD_PID} || true
    fi
    echo -e "\n\n`date '+%Y-%m-%d %H:%M:%S'`: run-tests.sh exiting\n\n"
}
trap cleanup EXIT

echo -e "\n\n`date '+%Y-%m-%d %H:%M:%S'`: run-tests.sh starting\n\n"

if [ -n "${CONFORMANCE_K8_NAMESPACE}" ]; then
    # to run tests against a cloud environment, you need to:
    # 1. install the gcloud tools
    # 2. gcloud auth login
    # 3. gcloud config set project conformance-suite
    # 4. gcloud container clusters  get-credentials conformance-suite-cluster-1 --zone us-central1-a
    # Then before running run-tests.sh do "source demo-environ.sh" (or whichever environment you want to run against)
    # to run against your local deployment, just don't do the 'source' command

    # This is just here so the cluster status before the tests run is recorded/available for diagnostics
    echo "Running kubectl get all for conformance suite namespace:"
    echo
    kubectl --namespace=${CONFORMANCE_K8_NAMESPACE} get all
    echo

    POD=`kubectl --namespace=${CONFORMANCE_K8_NAMESPACE} get pod -l app=${CONFORMANCE_K8_APPNAME} -o template --template="{{(index .items 0).metadata.name}}"`
    echo "Pod is: '$POD' - establishing port forward..."

    n=0
    while :; do
        kubectl --namespace=${CONFORMANCE_K8_NAMESPACE} port-forward $POD ${CONFORMANCE_MICROAUTH_LOCAL_PORT}:${CONFORMANCE_MICROAUTH_REMOTE_PORT} &
        PORT_FORWARD_PID=$!
        ./wait-for.sh localhost:${CONFORMANCE_MICROAUTH_LOCAL_PORT} && break
        # failed; it's possible the pod hadn't started, wait-for.sh already delayed 15 seconds so hopefully it might work if we try again
        # eg. https://gitlab.com/fintechlabs/fapi-conformance-suite/-/jobs/140077859
        if [ $n -ge 5 ]; then
            echo "Port forward not established after $n attempts; giving up"
            exit 1
        fi
        n=$[$n+1]
    done
fi

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
    # ciba
    TESTS="${TESTS} fapi-ciba-poll-with-mtls-test-plan authlete-fapi-ciba-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-poll-test-plan authlete-fapi-ciba-privatekey-poll.json"
    # ping/push can't currently be part of the regression test; see https://gitlab.com/openid/conformance-suite/issues/389
    #TESTS="${TESTS} fapi-ciba-ping-with-mtls-test-plan authlete-fapi-ciba-mtls-ping.json"
    #TESTS="${TESTS} fapi-ciba-push-with-mtls-test-plan authlete-fapi-ciba-mtls-push.json"

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

if [ "$#" -eq 0 ]; then
    TESTS="${TESTS} --show-untested-test-modules all"
    echo "Run all tests"
    makeServerTest
    makeClientTest
elif [[ ("$#" -eq 1 ) &&  ("$1" = "--client-tests-only" ) ]]; then
    TESTS="${TESTS} --show-untested-test-modules client"
    echo "Run client tests"
    makeClientTest
elif [[ ("$#" -eq 1) && ("$1" = "--server-tests-only") ]]; then
    TESTS="${TESTS} --show-untested-test-modules server"
    echo "Run server tests"
    makeServerTest
else
    echo "Syntax: run-tests.sh [--client-tests-only|--server-tests-only]"
    exit 1
fi

../conformance-suite/scripts/run-test-plan.py $TESTS
