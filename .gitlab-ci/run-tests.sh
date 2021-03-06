#!/bin/sh

set -e
cleanup() {
    echo
    echo
    echo "`date '+%Y-%m-%d %H:%M:%S'`: run-tests.sh exiting"
    echo
    echo
}
trap cleanup EXIT

echo
echo
echo "`date '+%Y-%m-%d %H:%M:%S'`: run-tests.sh starting"
echo
echo

# to run tests against a cloud environment, you need to:
# 1. create an API token
# 2. set the CONFORMANCE_TOKEN environment variable to the token
# 3. do "source demo-environ.sh" (or whichever environment you want to run against)
# (to run against your local deployment, just don't do the 'source' command)

export TEST_CONFIG_ALIAS='test/a/fintech-clienttest/'
export OIDCC_TEST_CONFIG_ALIAS='test/a/openidfoundationinternal-clienttest/'
export ACCOUNTS='test-mtls/a/fintech-clienttest/open-banking/v1.1/accounts'
export ACCOUNT_REQUEST='test/a/fintech-clienttest/open-banking/v1.1/account-requests'
export BRAZIL_CONSENT_REQUEST='test/a/fintech-clienttest/consents/v1/consents'

TESTS=""
EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-server.json|../conformance-suite/.gitlab-ci/expected-failures-ciba.json|../conformance-suite/.gitlab-ci/expected-failures-client.json"
EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-server.json|../conformance-suite/.gitlab-ci/expected-skips-ciba.json|../conformance-suite/.gitlab-ci/expected-skips-client.json"

makeClientTest() {
    . node-client-setup.sh
    . node-core-client-setup.sh

    # client FAPI1-ADVANCED
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-brazil-client-test.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-brazil-client-test.json"
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_auth_request_method=pushed][fapi_response_mode=jarm][fapi_jarm_type=plain_oauth] automated-brazil-client-test-no-openid-scope.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_auth_request_method=pushed][fapi_response_mode=jarm][fapi_jarm_type=oidc] automated-ob-client-test.json"
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-ob-client-test.json"
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=jarm][fapi_jarm_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=jarm][fapi_jarm_type=plain_oauth] automated-ob-client-test-no-openid-scope.json"


    # client FAPI-RW-ID2
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-ob-client-test.json"

    # client FAPI-RW-ID2-OB
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc] automated-ob-client-test.json"

    # client OpenID Connect Core Client Tests
    TESTS="${TESTS} oidcc-client-test-plan(../conformance-suite/.gitlab-ci/oidcc-rp-tests-config.json) automated-oidcc-client-test.json"
    # OIDC Core RP refresh token tests
    TESTS="${TESTS} oidcc-client-refreshtoken-test-plan(../conformance-suite/.gitlab-ci/oidcc-rp-refreshtoken-test-plan-config.json) automated-oidcc-client-test.json"
}

makeServerTest() {
    # OIDCC certification tests - static server, static client configuration
    TESTS="${TESTS} oidcc-basic-certification-test-plan[server_metadata=static][client_registration=static_client] authlete-oidcc-secret-basic-server-static.json"
    TESTS="${TESTS} oidcc-implicit-certification-test-plan[server_metadata=static][client_registration=static_client] authlete-oidcc-secret-basic-server-static.json"
    TESTS="${TESTS} oidcc-hybrid-certification-test-plan[server_metadata=static][client_registration=static_client] authlete-oidcc-secret-basic-server-static.json"
    #TESTS="${TESTS} oidcc-formpost-basic-certification-test-plan[server_metadata=static][client_registration=static_client] authlete-oidcc-secret-basic-server-static.json"
    #TESTS="${TESTS} oidcc-formpost-implicit-certification-test-plan[server_metadata=static][client_registration=static_client] authlete-oidcc-secret-basic-server-static.json"
    TESTS="${TESTS} oidcc-formpost-hybrid-certification-test-plan[server_metadata=static][client_registration=static_client] authlete-oidcc-secret-basic-server-static.json"

    # OIDCC certification tests - server supports discovery, static client
    TESTS="${TESTS} oidcc-basic-certification-test-plan[server_metadata=discovery][client_registration=static_client] authlete-oidcc-secret-basic.json"
    TESTS="${TESTS} oidcc-implicit-certification-test-plan[server_metadata=discovery][client_registration=static_client] authlete-oidcc-secret-basic.json"
    TESTS="${TESTS} oidcc-hybrid-certification-test-plan[server_metadata=discovery][client_registration=static_client] authlete-oidcc-secret-basic.json"
    TESTS="${TESTS} oidcc-config-certification-test-plan authlete-oidcc-secret-basic.json"
    TESTS="${TESTS} oidcc-formpost-basic-certification-test-plan[server_metadata=discovery][client_registration=static_client] authlete-oidcc-secret-basic.json"
    #TESTS="${TESTS} oidcc-formpost-implicit-certification-test-plan[server_metadata=discovery][client_registration=static_client] authlete-oidcc-secret-basic.json"
    #TESTS="${TESTS} oidcc-formpost-hybrid-certification-test-plan[server_metadata=discovery][client_registration=static_client] authlete-oidcc-secret-basic.json"

    # OIDCC certification tests - server supports discovery, using dcr
    TESTS="${TESTS} oidcc-basic-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    TESTS="${TESTS} oidcc-implicit-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    TESTS="${TESTS} oidcc-hybrid-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    TESTS="${TESTS} oidcc-config-certification-test-plan authlete-oidcc-dcr.json"
    TESTS="${TESTS} oidcc-dynamic-certification-test-plan[response_type=code\ id_token] authlete-oidcc-dcr.json"
    TESTS="${TESTS} oidcc-3rdparty-init-login-certification-test-plan[response_type=code\ id_token] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-formpost-basic-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    TESTS="${TESTS} oidcc-formpost-implicit-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-formpost-hybrid-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] authlete-oidcc-dcr.json"

    # OIDCC
    # commented out tests removed as they don't test something significantly different, in order to keep the test time down
    # client_secret_basic - static client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][client_registration=static_client] authlete-oidcc-secret-basic.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-basic.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-basic.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-basic.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-basic.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-basic.json"

    # client_secret_basic - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"

    # client_secret_post - static client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code][response_mode=default][client_registration=static_client] authlete-oidcc-secret-post.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-post.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-post.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-post.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-post.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-post.json"

    # client_secret_post - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"

    # client_secret_jwt - static client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=default][client_registration=static_client] authlete-oidcc-secret-jwt.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-jwt.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-jwt.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-jwt.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-jwt.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-secret-jwt.json"

    # client_secret_jwt - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"

    # private_key_jwt - static client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code][response_mode=default][client_registration=static_client] authlete-oidcc-privatekey.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token][response_mode=default][client_registration=static_client] authlete-oidcc-privatekey.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ token][response_mode=default][client_registration=static_client] authlete-oidcc-privatekey.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-privatekey.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token][response_mode=default][client_registration=static_client] authlete-oidcc-privatekey.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token\ token][response_mode=default][client_registration=static_client] authlete-oidcc-privatekey.json"

    # private_key_jwt - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] authlete-oidcc-dcr.json"

    # form post
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token][response_mode=form_post][client_registration=dynamic_client] authlete-oidcc-dcr.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token\ token][response_mode=form_post][client_registration=dynamic_client] authlete-oidcc-dcr.json"

    # Brazil FAPI Dynamic client registration
    TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=mtls][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-brazil-dcr.json"
    #TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-brazil-dcr.json"
    #TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=mtls][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-dcr.json"
    #TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-dcr.json"
    #TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=mtls][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi-brazil-dcr.json"
    TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi-brazil-dcr.json"
    TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=mtls][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi-brazil-dcr.json"
    #TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi-brazil-dcr.json"

    # Brazil FAPI
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-brazil-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-brazil-privatekey.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-privatekey.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi-brazil-privatekey-jarm.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi-brazil-privatekey-jarm.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi-brazil-mtls-jarm.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi-brazil-mtls-jarm.json"

    # authlete openbanking
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-ob-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-ob-privatekey.json"

    # authlete FAPI (request object by value)
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-privatekey.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=consumerdataright_au][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-privatekey-encryptedidtoken.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-mtls-jarm.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-privatekey-jarm.json"

    # authlete FAPI (PAR)
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-rw-id2-mtls.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-rw-id2-privatekey.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi-rw-id2-mtls-jarm.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi-rw-id2-privatekey-jarm.json"

    TESTS="${TESTS} fapi-r-test-plan[fapir_client_auth_type=mtls] authlete-fapi-r-mtls.json"
    TESTS="${TESTS} fapi-r-test-plan[fapir_client_auth_type=private_key_jwt] authlete-fapi-r-private-key.json"
    TESTS="${TESTS} fapi-r-test-plan[fapir_client_auth_type=client_secret_jwt] authlete-fapi-r-client-secret.json"
    TESTS="${TESTS} fapi-r-test-plan[fapir_client_auth_type=none] authlete-fapi-r-pkce.json"

    # This is the configuration used in the instructions as an example.
    # We keep it here as we want to be sure code changes don't break the example in the instructions, but the downside is there
    # is a chance that users may be using the alias at the same time our tests are running
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-privatekey-for-instructions.json"
}

makeCIBATest() {
    # ciba
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-privatekey-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-privatekey-poll.json"

    # ciba poll DCR
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"

    # ciba ping DCR
    # only one backchannel notification endpoint is allowed in CIBA so DCR must be used for ping testing
    # see https://gitlab.com/openid/conformance-suite/issues/389
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    # push isn't allowed in FAPI-CIBA profile
    #TESTS="${TESTS} fapi-ciba-id1-push-with-mtls-test-plan authlete-fapi-ciba-id1-mtls-push.json"
}

makeLocalProviderTests() {
    # OIDCC certification tests - server supports discovery, using dcr
    TESTS="${TESTS} oidcc-basic-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-implicit-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-hybrid-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-config-certification-test-plan ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-dynamic-certification-test-plan[response_type=code\ id_token] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-3rdparty-init-login-certification-test-plan[response_type=code\ id_token] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-rp-initiated-logout-certification-test-plan[response_type=code\ id_token][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-frontchannel-rp-initiated-logout-certification-test-plan[response_type=code\ id_token][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-backchannel-rp-initiated-logout-certification-test-plan[response_type=code\ id_token][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-formpost-basic-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-formpost-implicit-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-formpost-hybrid-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # OIDCC
    # client_secret_basic - dynamic client
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # client_secret_post - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # client_secret_jwt - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # private_key_jwt - dynamic client
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # form post
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token][response_mode=form_post][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=form_post][client_registration=dynamic_client] ../conformance-suite/.gitlab-ci/local-provider-oidcc-conformance-config.json"

}

if [ "$#" -eq 0 ]; then
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    # ignore that logout tests are untested (Authlete doesn't support the RP initiated logout specs)
    TESTS="${TESTS} --show-untested-test-modules all-except-logout"
    TESTS="${TESTS} --export-dir ../conformance-suite"
    echo "Run all tests"
    makeServerTest
    makeCIBATest
    makeClientTest
elif [ "$#" -eq 1 ] && [ "$1" = "--client-tests-only" ]; then
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-client.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-client.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules client"
    TESTS="${TESTS} --export-dir ../conformance-suite"
    echo "Run client tests"
    makeClientTest
elif [ "$#" -eq 1 ] && [ "$1" = "--server-tests-only" ]; then
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-server.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-server.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    # ignore that logout tests are untested (Authlete doesn't support the RP initiated logout specs)
    TESTS="${TESTS} --show-untested-test-modules server-authlete"
    TESTS="${TESTS} --export-dir ../conformance-suite"
    echo "Run server tests"
    makeServerTest
elif [ "$#" -eq 1 ] && [ "$1" = "--ciba-tests-only" ]; then
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-ciba.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-ciba.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules ciba"
    TESTS="${TESTS} --export-dir ../conformance-suite"
    echo "Run ciba tests"
    makeCIBATest
elif [ "$#" -eq 1 ] && [ "$1" = "--local-provider-tests" ]; then
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-local.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-local.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules server-oidc-provider"
    TESTS="${TESTS} --export-dir ."
    echo "Run local provider tests"
    makeLocalProviderTests
else
    echo "Syntax: run-tests.sh [--client-tests-only|--server-tests-only|--ciba-tests-only|--local-provider-tests]"
    exit 1
fi

echo ${TESTS} | xargs ../conformance-suite/scripts/run-test-plan.py
