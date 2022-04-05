#!/bin/bash

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

TESTS=""
EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-server.json|../conformance-suite/.gitlab-ci/expected-failures-ciba.json|../conformance-suite/.gitlab-ci/expected-failures-client.json"
EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-server.json|../conformance-suite/.gitlab-ci/expected-skips-ciba.json|../conformance-suite/.gitlab-ci/expected-skips-client.json"

makeClientTest() {
    . ./node-client-setup.sh
    . ./node-core-client-setup.sh

    #BRAZIL (note brazil_client_scope is not a variant but is used to pass scopes to tests. brazil_client_scope values use - instead of space)
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc][brazil_client_scope=openid-payments] automated-brazil-client-test.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_jarm_type=oidc][brazil_client_scope=openid-payments] automated-brazil-client-test-payments.json"

    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_jarm_type=oidc][brazil_client_scope=openid-accounts] automated-brazil-client-test.json"
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_jarm_type=oidc][brazil_client_scope=openid-accounts] automated-brazil-client-test.json"

    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=by_value][fapi_response_mode=jarm][fapi_jarm_type=plain_oauth][brazil_client_scope=accounts] automated-brazil-client-test.json"

    # client FAPI1-ADVANCED
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
    # FAPI2 baseline
    TESTS="${TESTS} fapi2-baseline-id2-test-plan[client_auth_type=private_key_jwt][sender_constrain=dpop][fapi_profile=openbanking_brazil] authlete-fapi2baseline-brazil-privatekey-dpop.json"
    TESTS="${TESTS} fapi2-baseline-id2-test-plan[client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=openbanking_brazil] authlete-fapi2baseline-brazil-privatekey.json"

    # FAPI2 advanced
    TESTS="${TESTS} fapi2-advanced-id1-test-plan[client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_response_mode=jarm][fapi_profile=openbanking_brazil] authlete-fapi2baseline-brazil-privatekey-jarm.json"
    # We don't have access to a server that supports 'signed_non_repudiation' yet
    #TESTS="${TESTS} fapi2-advanced-id1-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=openbanking_brazil] authlete-fapi2baseline-brazil-privatekey.json"

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

    # OP tests run against RP tests
    # MTLS
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_jarm_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # private_key_jwt
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_jarm_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # PAR
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_jarm_type=oidc][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # JARM
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_jarm_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # Brazil
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_jarm_type=oidc][fapi_auth_request_method=by_value][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"

    # Brazil DCR (mtls)
    TESTS="${TESTS} fapi1-advanced-final-brazil-client-test-plan[client_auth_type=mtls][fapi_jarm_type=oidc][fapi_response_mode=plain_response]:fapi1-advanced-final-client-brazildcr-happypath-test[fapi_auth_request_method=by_value][fapi_profile=openbanking_brazil]{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]:fapi1-advanced-final-brazildcr-happy-flow[fapi_profile=openbanking_brazil]}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"
    # Brazil DCR (mtls+PAR)
    TESTS="${TESTS} fapi1-advanced-final-brazil-client-test-plan[client_auth_type=mtls][fapi_jarm_type=oidc][fapi_response_mode=plain_response]:fapi1-advanced-final-client-brazildcr-happypath-test[fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil]{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_response_mode=plain_response]:fapi1-advanced-final-brazildcr-happy-flow[fapi_profile=openbanking_brazil]}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"
    # Brazil DCR accounts (private_key_jwt)
    TESTS="${TESTS} fapi1-advanced-final-brazil-client-test-plan[client_auth_type=private_key_jwt][fapi_jarm_type=oidc][fapi_response_mode=plain_response]:fapi1-advanced-final-client-brazildcr-happypath-test[fapi_auth_request_method=by_value][fapi_profile=openbanking_brazil]{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]:fapi1-advanced-final-brazildcr-happy-flow[fapi_profile=openbanking_brazil]}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"
    # Brazil DCR payments (private_key_jwt)
    TESTS="${TESTS} fapi1-advanced-final-brazil-client-test-plan[client_auth_type=private_key_jwt][fapi_jarm_type=oidc][fapi_response_mode=plain_response]:fapi1-advanced-final-client-brazildcr-happypath-test[fapi_auth_request_method=by_value][fapi_profile=openbanking_brazil]{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]:fapi1-advanced-final-brazildcr-happy-flow[fapi_profile=openbanking_brazil]}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-payments-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-payments.json"

    # FAPI2 OP against RP
    TESTS="${TESTS} fapi2-baseline-id2-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_jarm_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-baseline-id2-client-test{fapi2-advanced-id1-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-baseline-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"

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

makeEkycTests() {
    TESTS="${TESTS} ekyc-test-plan-oidccore[client_auth_type=mtls][server_metadata=discovery][response_type=code][client_registration=static_client][response_mode=default] yesdotcom-ekyc.json"
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

TESTS="${TESTS} --verbose"
if [ "$#" -eq 0 ]; then
    echo "Run all tests"
    makeServerTest
    makeCIBATest
    makeClientTest
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    # ignore that logout tests are untested (Authlete doesn't support the RP initiated logout specs)
    TESTS="${TESTS} --show-untested-test-modules all-except-logout"
    TESTS="${TESTS} --export-dir ../conformance-suite"
elif [ "$#" -eq 1 ] && [ "$1" = "--client-tests-only" ]; then
    echo "Run client tests"
    makeClientTest
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-client.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-client.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules client"
    TESTS="${TESTS} --export-dir ../conformance-suite"
elif [ "$#" -eq 1 ] && [ "$1" = "--server-tests-only" ]; then
    echo "Run server tests"
    makeServerTest
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-server.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-server.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    # ignore that logout tests are untested (Authlete doesn't support the RP initiated logout specs)
    TESTS="${TESTS} --show-untested-test-modules server-authlete"
    TESTS="${TESTS} --export-dir ../conformance-suite"
    TESTS="${TESTS} --no-parallel-for-no-alias" # the jobs without aliases aren't the slowest queue, so avoid overwhelming server early on
elif [ "$#" -eq 1 ] && [ "$1" = "--ciba-tests-only" ]; then
    echo "Run ciba tests"
    makeCIBATest
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-ciba.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-ciba.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules ciba"
    TESTS="${TESTS} --export-dir ../conformance-suite"
    TESTS="${TESTS} --no-parallel" # the authlete authentication device simulator doesn't seem to support parallel authorizations
elif [ "$#" -eq 1 ] && [ "$1" = "--ekyc-tests" ]; then
    echo "Run eKYC tests"
    makeEkycTests
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-ekyc.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-ekyc.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules ekyc"
    TESTS="${TESTS} --export-dir ../conformance-suite"
elif [ "$#" -eq 1 ] && [ "$1" = "--local-provider-tests" ]; then
    echo "Run local provider tests"
    makeLocalProviderTests
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-local.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-local.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules server-oidc-provider"
    TESTS="${TESTS} --export-dir ."
else
    echo "Syntax: run-tests.sh [--client-tests-only|--server-tests-only|--ciba-tests-only|--local-provider-tests||--ekyc-tests]"
    exit 1
fi

echo ${TESTS} | xargs ../conformance-suite/scripts/run-test-plan.py
