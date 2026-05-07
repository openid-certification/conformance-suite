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

# Resolve the conformance-suite repo root from this script's location
SUITE_DIR="$(cd "$(dirname "$0")/.." && pwd)"

TESTS=""
EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-oidcc.json|${SUITE_DIR}/.gitlab-ci/expected-failures-fapi.json|${SUITE_DIR}/.gitlab-ci/expected-failures-vc.json|${SUITE_DIR}/.gitlab-ci/expected-failures-ciba.json|${SUITE_DIR}/.gitlab-ci/expected-failures-client.json"
EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-oidcc.json|${SUITE_DIR}/.gitlab-ci/expected-skips-fapi.json|${SUITE_DIR}/.gitlab-ci/expected-skips-vc.json|${SUITE_DIR}/.gitlab-ci/expected-skips-ciba.json|${SUITE_DIR}/.gitlab-ci/expected-skips-client.json"

makeClientTest() {
    . ./node-client-setup.sh
    . ./node-core-client-setup.sh
    . ./ksa-client-setup.sh

    #BRAZIL (note brazil_client_scope is not a variant but is used to pass scopes to tests. brazil_client_scope values use - instead of space)
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_client_type=oidc][brazil_client_scope=openid-payments] automated-brazil-client-test-payments.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_client_type=oidc][brazil_client_scope=openid-accounts] automated-brazil-client-test.json"

    # client FAPI1-ADVANCED
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_brazil][fapi_auth_request_method=pushed][fapi_response_mode=jarm][fapi_client_type=plain_oauth] automated-brazil-client-test-no-openid-scope.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_client_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_auth_request_method=pushed][fapi_response_mode=jarm][fapi_client_type=oidc] automated-ob-client-test.json"
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_client_type=oidc] automated-ob-client-test.json"
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=jarm][fapi_client_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=jarm][fapi_client_type=plain_oauth] automated-ob-client-test-no-openid-scope.json"

    # client OpenID Connect Core Client Tests
    # Coverage strategy: one plan per auth type (code), plus one each of
    # code token, code id_token token, and id_token token (all via client_secret_basic).
    # form_post and request_object tested once each via code.
    # Hybrid variants for non-client_secret_basic auth types removed to reduce CI time (~52m -> ~32m).
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_encrypted_response_alg=A128KW][userinfo_encrypted_response_alg=A128GCMKW][request_object_encryption_alg=RSA-OAEP]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[userinfo_encrypted_response_alg=ECDH-ES][userinfo_signed_response_alg=ES256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_signed_response_alg=PS256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_signed_response_alg=ES256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_post][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_signed_response_alg=ES256K][userinfo_signed_response_alg=EdDSA]}_ automated-oidcc-client-test.json"
    #TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_post][response_type=code id_token token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_encrypted_response_alg=RSA-OAEP][id_token_encrypted_response_enc=A192CBC-HS384][userinfo_encrypted_response_alg=ECDH-ES][userinfo_encrypted_response_enc=A128CBC-HS256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[userinfo_encrypted_response_alg=ECDH-ES][userinfo_encrypted_response_enc=A192CBC-HS384]}_ automated-oidcc-client-test.json"
    #TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=private_key_jwt][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[token_endpoint_auth_signing_alg=ES256]}_ automated-oidcc-client-test.json"
    #TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[token_endpoint_auth_signing_alg=ES256]}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=none][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    #TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=none][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    #TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=none][response_type=id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=form_post][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    #TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code id_token token][response_mode=form_post][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][request_type=request_object][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    #TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][request_type=request_object][client_registration=dynamic_client]{sample-openid-client-nodejs[request_object_encryption_alg=ECDH-ES]}_ automated-oidcc-client-test.json"

    TESTS="${TESTS}  oidcc-client-test-plan[client_auth_type=tls_client_auth][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[tls_client_auth_subject_dn=cn=test.certification.example.com,o=oidf,l=san ramon,st=ca,c=us](CLIENT_CERT=${SUITE_DIR}/.gitlab-ci/rp_tests-tls_client_auth.crt)(CLIENT_CERT_KEY=${SUITE_DIR}/.gitlab-ci/rp_tests-tls_client_auth.key)}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=self_signed_tls_client_auth][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs(CLIENT_CERT=${SUITE_DIR}/.gitlab-ci/rp_tests-tls_client_auth.crt)(CLIENT_CERT_KEY=${SUITE_DIR}/.gitlab-ci/rp_tests-tls_client_auth.key)}_ automated-oidcc-client-test.json"


    # OIDC Core RP refresh token tests
    TESTS="${TESTS} oidcc-client-refreshtoken-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"


    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_ksa][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_client_type=oidc] ./ksa-rp-client/fapi-ksa-rp-test-config-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_ksa][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_client_type=oidc] ./ksa-rp-client/fapi-ksa-rp-test-config-mtls.json"
}

_makeFapiTestPart1() {

    # FAPI2 security profile
    # client credentials grant
    TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=mtls][sender_constrain=mtls][fapi_profile=fapi_client_credentials_grant] authlete-fapi2securityprofile-mtls-plainoauth.json"

    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=fapi_client_credentials_grant] authlete-fapi2securityprofile-privatekey-plainoauth.json"
    TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][sender_constrain=dpop][fapi_profile=fapi_client_credentials_grant] authlete-fapi2securityprofile-client-credentials-privatekey-dpop.json"

    # FAPI2 security profile
    # plain oauth
    TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=mtls][sender_constrain=mtls][fapi_profile=plain_fapi] authlete-fapi2securityprofile-mtls-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=plain_fapi] authlete-fapi2securityprofile-privatekey-plainoauth.json"

    # oidc
    TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=plain_fapi] authlete-fapi2securityprofile-privatekey.json"

    # FAPI2 message signing - jar
    TESTS="${TESTS} fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response] authlete-fapi2securityprofile-privatekey-jar.json"

    # FAPI2 message signing - jarm
    TESTS="${TESTS} fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_response_mode=jarm][fapi_profile=plain_fapi] authlete-fapi2securityprofile-privatekey-jarm.json"

    # Brazil
    TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][sender_constrain=dpop][fapi_profile=openbanking_brazil] authlete-fapi2securityprofile-brazil-privatekey-dpop.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=openbanking_brazil] authlete-fapi2securityprofile-brazil-privatekey-plainoauth.json"

    # FAPI2 message signing - jar + connectid
    TESTS="${TESTS} fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response] authlete-fapi2securityprofile-connectid-privatekey.json"

    # FAPI2 message signing - jarm
    TESTS="${TESTS} fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_response_mode=jarm][fapi_profile=openbanking_brazil] authlete-fapi2securityprofile-brazil-privatekey-jarm.json"

    #FAPI2 CBUAE - jar + rar
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-par-without-duplicate-parameters}${SUITE_DIR}/scripts/test-configs-rp-against-op/cbuae-op.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/cbuae-rp.json"
    #TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-par-without-duplicate-parameters}${SUITE_DIR}/scripts/test-configs-rp-against-op/cbuae-op.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/cbuae-rp.json"
    TESTS="${TESTS} fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar] authlete-fapi2securityprofile-privatekey-jar-cbuae.json"
}

makeOidccTest() {
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
}

_makeFapiTestPart2() {
    # Brazil FAPI Dynamic client registration
    TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[fapi_profile=openbanking_brazil][client_auth_type=private_key_jwt][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-dcr.json"

    # Brazil FAPI
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-privatekey-encryptedidtoken.json"

    # authlete openbanking uk
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi1-adv-final-ob-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi1-adv-final-ob-privatekey.json"

    # authlete KSA/SAMA openbanking
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_ksa][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi1-final-mtls-sama.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_ksa][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi1-final-privatekey-sama.json"

    # authlete FAPI (request object by value)
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi1-adv-final-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi1-adv-final-privatekey.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=consumerdataright_au][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-au-cdr-fapi1-adv-final-privatekey-jarm.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi1-adv-final-mtls-jarm.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=by_value] authlete-fapi1-adv-final-privatekey-jarm.json"

    # authlete FAPI (PAR)
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi1-adv-final-mtls.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi1-adv-final-privatekey.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi1-adv-final-mtls-jarm.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi1-final-privatekey-jarm-encrypted.json"

    # OP tests run against RP tests
    # MTLS
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_client_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # private_key_jwt
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # PAR
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # JARM
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_client_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # Brazil OB
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"
    # Brazil OPIN
    # Temporarily disabled as it's failing - see https://gitlab.com/openid/conformance-suite/-/issues/1653
#    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=pushed][fapi_profile=openinsurance_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=openinsurance_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-opin.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-opin.json"

    # Brazil OB DCR accounts (private_key_jwt)
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openbanking_brazil]:fapi1-advanced-final-brazildcr-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts-dcr.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"
    # Brazil OB DCR payments (private_key_jwt)
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openbanking_brazil]:fapi1-advanced-final-brazildcr-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-payments-dcr.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-payments.json"
    # Brazil OB DCR payments (private_key_jwt). Payment consent request aud as array.
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openbanking_brazil]:fapi1-advanced-final-brazildcr-payment-consent-request-aud-as-array}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-payments-dcr.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-payments.json"
    # Brazil DCR OPIN (private_key_jwt)
    # Temporarily disabled as it's failing - see https://gitlab.com/openid/conformance-suite/-/issues/1653
    #TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openinsurance_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openinsurance_brazil]:fapi1-advanced-final-brazildcr-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-opin-dcr.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-opin-dcr.json"

    # FAPI2 Advanced OP against RP
      # fapi_client_credentials_grant
         # client_auth=private_key, sender_constrain=mtls
    TESTS="${TESTS} fapi2-security-profile-final-client-test-plan[client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=fapi_client_credentials_grant][fapi_client_type=plain_oauth]:fapi2-security-profile-final-client-test-happy-path{fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=fapi_client_credentials_grant]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-access-token-type-header-case-sensitivity}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-no-openid.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config-no-openid.json"

    # FAPI2 Advanced OP against RP
      # fapi_client_credentials_grant
         # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} fapi2-security-profile-final-client-test-plan[client_auth_type=mtls][sender_constrain=dpop][fapi_profile=fapi_client_credentials_grant][fapi_client_type=plain_oauth]:fapi2-security-profile-final-client-test-happy-path-no-dpop-nonce{fapi2-security-profile-final-test-plan[openid=plain_oauth][client_auth_type=mtls][sender_constrain=dpop][fapi_profile=fapi_client_credentials_grant]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-access-token-type-header-case-sensitivity}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-dpop-no-openid.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config-no-openid.json"

    # FAPI2 Advanced OP against RP
      # plain_fapi, signed_non_repudiation, plain_response
         # client_auth=private_key, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
         # client_auth=private_key, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-no-mtls.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-no-mtls.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"

        # client_auth=mtls, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
        # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-mtls-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-mtls-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"


    # FAPI2 Advanced OP against RP using EdDSA signing alg
      # plain_fapi, signed_non_repudiation, plain_response
         # client_auth=private_key, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"

         # client_auth=private_key, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-no-mtls.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
         # client_auth=private_key, sender_constrain=dpop, dpop_signing_alg=es256
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-no-mtls-dpop-es256.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-no-mtls.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-no-mtls-dpop-es256.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"

        # client_auth=mtls, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
        # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-mtls-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
        # client_auth=mtls, sender_constrain=dpop, dpop_signing_alg=es256
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-mtls-client-auth-dpop-es256.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-mtls-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-mtls-client-auth-dpop-es256.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"


      # connectid, signed_non_repudiation, plain_response, mtls - connectid discovery+happyflow
    # connectid discovery+happyflow
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-rp-test-config.json"
    # connectid returning identity claims
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-australia-connectid-test-claims-parameter-idtoken-identity-claims}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-rp-test-config-1.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-australia-connectid-test-claims-parameter-idtoken-identity-claims}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-op-test-config-1.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-au-connectid-rp-test-config-1.json"

    # plain fapi without openid connect
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=plain_oauth][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-no-openid.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config-no-openid.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=plain_oauth][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-no-openid.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config-no-openid.json"

      # FAPI2 baseline OP against RP
      # These don't use the baseline test plans as currently the run-test-plan.py syntax for selecting individual models doesn't work for these plans, it ends up specifying extra unnecessary variants when scheduling the test
         # client_auth=private_key, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
         # client_auth=private_key, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-no-mtls.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
        # client_auth=mtls, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=unsigned][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
        # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=unsigned][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-mtls-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-par-without-duplicate-parameters}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-par-without-duplicate-parameters}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-no-mtls.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=mtls][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=unsigned][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-par-without-duplicate-parameters}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    TESTS="${TESTS} fapi2-message-signing-final-client-test-plan[client_auth_type=mtls][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-client-test-happy-path{fapi2-message-signing-final-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=unsigned][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-final-discovery-end-point-verification,fapi2-security-profile-final-par-without-duplicate-parameters}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-op-test-config-mtls-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"


    # This is the configuration used in the instructions as an example.
    # We keep it here as we want to be sure code changes don't break the example in the instructions, but the downside is there
    # is a chance that users may be using the alias at the same time our tests are running
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi1-adv-final-privatekey-for-instructions.json"
}

makeFapiTest() {
    _makeFapiTestPart1
    _makeFapiTestPart2
}

makeVcTests() {
    # OpenID4VP op-against-rp
    VPPROFILE="vp_profile=plain_vp"
    SDJWT="credential_format=sd_jwt_vc"
    MDL="credential_format=iso_mdl"
    SANDNS="client_id_scheme=x509_san_dns"
    SIGNEDREQ="request_method=request_uri_signed"
    DIRECTPOST="response_mode=direct_post"
    DCQL="query_language=dcql"
    PEX="query_language=presentation_exchange"
    CONFIGS="${SUITE_DIR}/scripts/test-configs-rp-against-op"

    # VP ID2
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-id2-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect-alt.json"

    # VP ID3 - DCQL
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$DCQL]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config-dcql-mdoc.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config-dcql-mdoc.json ${CONFIGS}/vp-verifier-test-config-with-redirect-alt.json"

    # VP ID3 - Presentation Exchange
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$PEX]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect-alt.json"

    # VP 1.0 Final
    SANDNS="client_id_prefix=x509_san_dns" # final changed scheme -> prefix
    HASH="client_id_prefix=x509_hash"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$HASH][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-minimal-cnf-jwk{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$HASH][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config-with-redirect-no-client-id.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-wallet-alternate-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$HASH][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$HASH][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-wallet-alternate-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config-with-redirect-no-client-id.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-mdoc.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-mdoc-mdl.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-invalid-session-transcript{oid4vp-1final-wallet-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-mdoc-negative.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-invalid-kb-jwt-signature{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-negative-kb-signature.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-invalid-credential-signature{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-negative-credential-signature.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-invalid-sd-hash{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-negative-sd-hash.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-invalid-kb-jwt-nonce{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-negative-nonce.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-invalid-kb-jwt-aud{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-negative-aud.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-kb-jwt-iat-in-past{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-negative-iat-past.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-kb-jwt-iat-in-future{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-negative-iat-future.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-wallet-alternate-happy-flow}${CONFIGS}/vp-wallet-test-config-dcql-mdoc.json ${CONFIGS}/vp-verifier-test-config-with-redirect-alt.json"
    # VP 1.0 Final - DC API (mock wallet response, no paired verifier needed)
    # Module name listed 3 times so the runner picks up all three dc_api.jwt HAIP
    # entries: unsigned (web-origin), signed (x509_hash), and multi-signed (x509_hash).
    DCAPIJWT="response_mode=dc_api.jwt"
    TESTS="${TESTS} oid4vp-1final-wallet-haip-test-plan[$SDJWT][$DCAPIJWT]:oid4vp-1final-wallet-alternate-happy-flow,oid4vp-1final-wallet-alternate-happy-flow,oid4vp-1final-wallet-alternate-happy-flow ${CONFIGS}/vp-wallet-test-config-dcql-sdjwt-dcapi-mock.json"

    # VP 1.0 Final - request_uri_method=post
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-request-uri-method-post{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-request-uri-method-post}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config.json"
    # VP 1.0 Final - wallet negative test: unknown transaction_data type
    # Uses a dedicated verifier config so the expected-failure entries (verifier-side rejection of transaction_data) only apply to this paired run.
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-negative-test-unknown-transaction-data-type}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config-transaction-data.json"
    # VP 1.0 Final - request_uri_method=post + direct_post.jwt (encrypted response)
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-verifier-request-uri-method-post{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-1final-wallet-request-uri-method-post}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config.json"
    # VP 1.0 Final - DCQL variation tests (fewer claims, optional credential_set, no claims)
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-fewer-claims-than-available}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-optional-credential-set}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-no-claims-in-dcql-query}${CONFIGS}/vp-wallet-test-config-dcql-sdjwt.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-fewer-claims-than-available}${CONFIGS}/vp-wallet-test-config-dcql-mdoc.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-optional-credential-set}${CONFIGS}/vp-wallet-test-config-dcql-mdoc.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-1final-verifier-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-verifier-happy-flow{oid4vp-1final-wallet-test-plan[$VPPROFILE][$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-1final-wallet-no-claims-in-dcql-query}${CONFIGS}/vp-wallet-test-config-dcql-mdoc.json ${CONFIGS}/vp-verifier-test-config.json"

    # OpenID4VCI op-against-rp
    SIMPLE="authorization_request_type=simple"
    RAR="authorization_request_type=rar"
    PRIVATE_KEY="client_auth_type=private_key_jwt"
    MTLSAUTH="client_auth_type=mtls"
    CLIATTAUTH="client_auth_type=client_attestation"
    UNSIGNED="fapi_request_method=unsigned"
    MTLSAT="sender_constrain=mtls"
    DPOP="sender_constrain=dpop"
    WALLETINIT="vci_authorization_code_flow_variant=wallet_initiated"
    ISSUERINIT="vci_authorization_code_flow_variant=issuer_initiated"
    AUTHCODE="vci_grant_type=authorization_code"
    PREAUTHCODE="vci_grant_type=pre_authorization_code"
    OFFERBYVALUE="vci_credential_offer_variant=by_value"
    OFFERBYREF="vci_credential_offer_variant=by_reference"
    HAIP="fapi_profile=vci_haip"
    PLAINVCI="fapi_profile=vci"
    CRED_RESP_ENCR_PLAIN="vci_credential_encryption=plain"
    CRED_RESP_ENCR_ENCRYPTED="vci_credential_encryption=encrypted"
    ISSUANCE_DEFERRED="vci_credential_issuance_mode=deferred"
    MDOC="credential_format=mdoc"
    SDJWTVC="credential_format=sd_jwt_vc"
    ISSUANCE_IMMEDIATE="vci_credential_issuance_mode=immediate"
    FAPI_OPENID="openid=plain_oauth"
    FAPI_RESPONSEMODE="fapi_response_mode=plain_response"

    # Authorization Code Flow
         # client_auth=private_key, sender_constrain=mtls
         # We also run the oid4vci-1_0-issuer-metadata-test-signed test-module here
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$PRIVATE_KEY][$UNSIGNED][$MTLSAT][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$PRIVATE_KEY][$UNSIGNED][$MTLSAT][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-metadata-test-signed,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
         # client_auth=private_key, sender_constrain=dpop
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$PRIVATE_KEY][$UNSIGNED][$DPOP][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$PRIVATE_KEY][$UNSIGNED][$DPOP][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-no-mtls.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
        # client_auth=mtls, sender_constrain=mtls
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$MTLSAUTH][$UNSIGNED][$MTLSAT][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$MTLSAUTH][$UNSIGNED][$MTLSAT][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
        # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$MTLSAUTH][$UNSIGNED][$DPOP][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$MTLSAUTH][$UNSIGNED][$DPOP][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
        # client_auth=client_attestation, sender_constrain=dpop (uses dedicated HAIP plans)
    TESTS="${TESTS} oid4vci-1_0-wallet-haip-test-plan[$WALLETINIT][$OFFERBYVALUE][$SDJWTVC]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-haip-test-plan[$WALLETINIT][$SDJWTVC]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
        # client_auth=client_attestation, sender_constrain=dpop key-attestation
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$OFFERBYVALUE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop-key-attestation.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
        # client_auth=client_attestation, sender_constrain=dpop - issuer initiated (credential offer by value)
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$AUTHCODE][$OFFERBYVALUE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$AUTHCODE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
        # client_auth=client_attestation, sender_constrain=dpop - issuer initiated (credential offer by reference)
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$AUTHCODE][$OFFERBYREF][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$AUTHCODE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop-offer-by-ref.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
    # Pre-Authorized Code Flow (not covered by HAIP)
        # client_auth=mtls, sender_constrain=dpop, grant_type=pre_authorization_code - issuer initiated (credential offer by value), oid4vci-1_0-wallet-happy-path-with-scopes, because of authorization_request_type=simple requires a scope mapping in credential metadata
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$MTLSAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$PREAUTHCODE][$OFFERBYVALUE][$PLAINVCI][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$MTLSAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$PREAUTHCODE][$PLAINVCI][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-plain.json"
        # client_auth=mtls, sender_constrain=dpop, grant_type=pre_authorization_code - issuer initiated (credential offer by value)
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$RAR][$MTLSAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$PREAUTHCODE][$OFFERBYVALUE][$PLAINVCI][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$RAR][$MTLSAUTH][$UNSIGNED][$DPOP][$ISSUERINIT][$PREAUTHCODE][$PLAINVCI][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-plain.json"
    # Deferred issuance - client_auth=client_attestation, sender_constrain=dpop
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$OFFERBYVALUE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_DEFERRED]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop-deferred.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
    # Encrypted credentials - client_auth=client_attestation, sender_constrain=dpop (uses nobinding credential)
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$OFFERBYVALUE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_ENCRYPTED][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$HAIP][$SDJWTVC][$CRED_RESP_ENCR_ENCRYPTED][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop-nobinding.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"
    # mdoc credential format - client_auth=client_attestation, sender_constrain=dpop
    TESTS="${TESTS} oid4vci-1_0-wallet-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$OFFERBYVALUE][$HAIP][$MDOC][$CRED_RESP_ENCR_PLAIN][$ISSUANCE_IMMEDIATE]:oid4vci-1_0-wallet-test-credential-issuance{oid4vci-1_0-issuer-test-plan[$SIMPLE][$CLIATTAUTH][$UNSIGNED][$DPOP][$WALLETINIT][$AUTHCODE][$HAIP][$MDOC][$CRED_RESP_ENCR_PLAIN][$FAPI_OPENID][$FAPI_RESPONSEMODE]:oid4vci-1_0-issuer-metadata-test,oid4vci-1_0-issuer-happy-flow}${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-issuer-test-config-client_attestation-client-auth-dpop-mdoc.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/vci-wallet-test-config-haip.json"

    # Authlete Issuer tests
    TESTS="${TESTS} oid4vci-1_0-issuer-haip-test-plan[$WALLETINIT][$SDJWTVC] authlete-vci-haip.json"
}

makeCIBATest() {
    # ciba poll - static client
    # Coverage: one plan per auth type for plain_fapi (static), plus
    # private_key_jwt/openbanking_uk (static). mtls/openbanking_uk/static
    # dropped — mtls/openbanking_uk covered by DCR variant below.
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_ciba_profile=plain_fapi][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=plain_fapi][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-privatekey-poll.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_ciba_profile=openbanking_uk][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-mtls-poll.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=openbanking_uk][ciba_mode=poll][client_registration=static_client] authlete-fapi-ciba-id1-privatekey-poll.json"

    # ciba poll DCR
    # private_key_jwt/plain_fapi/dynamic dropped — private_key_jwt/poll covered by
    # static variants above, and private_key_jwt/dynamic covered by ping plan below.
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_ciba_profile=plain_fapi][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=plain_fapi][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_ciba_profile=openbanking_uk][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=openbanking_uk][ciba_mode=poll][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"

    # ciba ping DCR
    # only one backchannel notification endpoint is allowed in CIBA so DCR must be used for ping testing
    # see https://gitlab.com/openid/conformance-suite/issues/389
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_ciba_profile=plain_fapi][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=plain_fapi][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_ciba_profile=openbanking_uk][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=openbanking_uk][ciba_mode=ping][client_registration=dynamic_client] authlete-fapi-ciba-id1-dcr.json"
    # push isn't allowed in FAPI-CIBA profile
    #TESTS="${TESTS} fapi-ciba-id1-push-with-mtls-test-plan authlete-fapi-ciba-id1-mtls-push.json"

    # FAPI CIBA OP against RP
    # MTLS
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=mtls][ciba_mode=poll][fapi_ciba_profile=plain_fapi]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_ciba_profile=plain_fapi][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-rp-test-config.json"
    # PKJWT
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=poll][fapi_ciba_profile=plain_fapi]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=plain_fapi][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-rp-test-config.json"
    # OFBR PKJWT
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=ping][fapi_ciba_profile=openbanking_brazil]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=openbanking_brazil][ciba_mode=ping][client_registration=static_client]:fapi-ciba-id1-brazil-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=ping][fapi_ciba_profile=openbanking_brazil]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=openbanking_brazil][ciba_mode=ping][client_registration=static_client]:fapi-ciba-id1-ensure-other-scope-order-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=ping][fapi_ciba_profile=openbanking_brazil]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=openbanking_brazil][ciba_mode=ping][client_registration=static_client]:fapi-ciba-id1-ensure-requested-expiry-is-ignored-for-brazil}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=ping][fapi_ciba_profile=openbanking_brazil]:fapi-ciba-id1-client-refresh-token-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_ciba_profile=openbanking_brazil][ciba_mode=ping][client_registration=static_client]:fapi-ciba-id1-brazil-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config-refresh.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
}

makeEkycTests() {

    # client_tauth_type = private_key_jwt
    TESTS="${TESTS} ekyc-test-plan-oidccore[client_auth_type=private_key_jwt][server_metadata=discovery][response_type=code][client_registration=dynamic_client][response_mode=default][security_profile=none][auth_request_method=http_query][auth_request_non_repudiation_method=unsigned][sender_constrain=none][fapi_response_mode=plain_response][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"

    # sender_constrain (none, dpop, mtls)
    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=http_query][response_type=code][auth_request_non_repudiation_method=unsigned][response_mode=default][fapi_response_mode=plain_response][sender_constrain=none][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=http_query][response_type=code][auth_request_non_repudiation_method=unsigned][response_mode=default][fapi_response_mode=plain_response][sender_constrain=dpop][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=http_query][response_type=code][auth_request_non_repudiation_method=unsigned][response_mode=default][fapi_response_mode=plain_response][sender_constrain=mtls][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"



    # auth_request_method = request_object_pushed, auth_request_non_repudiation_method = signed_non_repudiation
    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=default][fapi_response_mode=plain_response][sender_constrain=none][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=default][fapi_response_mode=plain_response][sender_constrain=dpop][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=default][fapi_response_mode=plain_response][sender_constrain=mtls][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"


    # auth_request_method = request_object_pushed, auth_request_non_repudiation_method = signed_non_repudiation
    # response_mode=form_post, fapi_response_mode=jarm
    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=form_post][fapi_response_mode=jarm][sender_constrain=none][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=form_post][fapi_response_mode=jarm][sender_constrain=dpop][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=private_key_jwt][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=form_post][fapi_response_mode=jarm][sender_constrain=mtls][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-privatekey-final.json"


    #--------------------------------------------

    # client_tauth_type = mtls
    TESTS="${TESTS} ekyc-test-plan-oidccore[client_auth_type=mtls][server_metadata=discovery][response_type=code][client_registration=dynamic_client][response_mode=default][security_profile=none][auth_request_method=http_query][auth_request_non_repudiation_method=unsigned][sender_constrain=none][fapi_response_mode=plain_response][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"


    # sender_constrain (none, dpop, mtls)
    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=http_query][response_type=code][auth_request_non_repudiation_method=unsigned][response_mode=default][fapi_response_mode=plain_response][sender_constrain=none][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=http_query][response_type=code][auth_request_non_repudiation_method=unsigned][response_mode=default][fapi_response_mode=plain_response][sender_constrain=dpop][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=http_query][response_type=code][auth_request_non_repudiation_method=unsigned][response_mode=default][fapi_response_mode=plain_response][sender_constrain=mtls][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"



    # auth_request_method = request_object_pushed, auth_request_non_repudiation_method = signed_non_repudiation
    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=default][fapi_response_mode=plain_response][sender_constrain=none][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=default][fapi_response_mode=plain_response][sender_constrain=dpop][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=default][fapi_response_mode=plain_response][sender_constrain=mtls][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"


    # auth_request_method = request_object_pushed, auth_request_non_repudiation_method = signed_non_repudiation
    # response_mode=form_post, fapi_response_mode=jarm
    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=form_post][fapi_response_mode=jarm][sender_constrain=none][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=form_post][fapi_response_mode=jarm][sender_constrain=dpop][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"

    TESTS="${TESTS} ekyc-test-plan-oidccore[server_metadata=discovery][client_registration=dynamic_client][security_profile=none][client_auth_type=mtls][auth_request_method=request_object_pushed][response_type=code][auth_request_non_repudiation_method=signed_non_repudiation][response_mode=form_post][fapi_response_mode=jarm][sender_constrain=mtls][ekyc_verified_claims_response_support=id_token_userinfo] authlete-ekyc-mtls.json"



}

makeFederationTests() {
    # OP vs RP tests
    TESTS="${TESTS} openid-federation-entity-joined-to-test-federation-rp-test-plan[server_metadata=discovery][client_registration=automatic]:openid-federation-client-test{ \
        openid-federation-entity-joined-to-test-federation-op-test-plan[server_metadata=discovery][client_registration=automatic]:\
        openid-federation-entity-configuration, openid-federation-automatic-client-registration-with-jar-and-get \
    } ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-rp-test-config.json \
    }"
    TESTS="${TESTS} openid-federation-entity-joined-to-test-federation-rp-test-plan[server_metadata=discovery][client_registration=automatic]:openid-federation-client-test{ \
        openid-federation-entity-joined-to-test-federation-op-test-plan[server_metadata=discovery][client_registration=automatic]:\
        openid-federation-automatic-client-registration-with-jar-and-post \
    } ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-rp-test-config.json \
    }"
    TESTS="${TESTS} openid-federation-entity-joined-to-test-federation-rp-test-plan[server_metadata=discovery][client_registration=automatic]:openid-federation-client-test{ \
        openid-federation-entity-joined-to-test-federation-op-test-plan[server_metadata=discovery][client_registration=automatic]:\
        openid-federation-automatic-client-registration-with-par \
    } ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-rp-test-config.json \
    }"
    TESTS="${TESTS} openid-federation-entity-joined-to-test-federation-rp-test-plan[server_metadata=discovery][client_registration=automatic]:openid-federation-client-test{ \
        openid-federation-entity-joined-to-test-federation-op-test-plan[server_metadata=discovery][client_registration=automatic]:\
        openid-federation-automatic-client-registration-with-jar-and-encrypted-request-object \
    } ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-op-test-config.json ${SUITE_DIR}/scripts/test-configs-rp-against-op/federation-rp-test-config.json \
    }"

    TESTS="${TESTS} openid-federation-deployed-entity-test-plan[server_metadata=discovery][client_registration=automatic] ${SUITE_DIR}/scripts/test-configs-federation/authlete-federation-fapidev-as.json"
    TESTS="${TESTS} openid-federation-deployed-entity-test-plan[server_metadata=discovery][client_registration=automatic] ${SUITE_DIR}/scripts/test-configs-federation/authlete-federation-trust-anchor.json"

    TESTS="${TESTS} openid-federation-entity-joined-to-test-federation-op-test-plan[server_metadata=discovery][client_registration=automatic] authlete-federation.json"
}

makeSsfTests() {

    PUSH_DELIVERY="ssf_delivery_mode=push"
    POLL_DELIVERY="ssf_delivery_mode=poll"
    CAEP_INTEROP_PROFILE="ssf_profile=caep_interop"

    STATIC_CLIENT="client_registration=static_client"
    SERVER_METADATA_STATIC="server_metadata=static"
    CLIENT_AUTH_CLIENT_SECRET_POST="client_auth_type=client_secret_post"
    SSF_METADATA="ssf_server_metadata=discovery"
    SSF_AUTH_MODE="ssf_auth_mode=static"

    TESTS="${TESTS} openid-ssf-receiver-test-plan[$PUSH_DELIVERY][$CAEP_INTEROP_PROFILE]:openid-ssf-receiver-happypath{openid-ssf-transmitter-test-plan[$PUSH_DELIVERY][$CAEP_INTEROP_PROFILE][$STATIC_CLIENT][$SERVER_METADATA_STATIC][$CLIENT_AUTH_CLIENT_SECRET_POST][$SSF_METADATA][$SSF_AUTH_MODE]:openid-ssf-transmitter-metadata,openid-ssf-stream-control-happy-path}${SUITE_DIR}/scripts/test-configs-ssf/ssf-transmitter-test-config.json ${SUITE_DIR}/scripts/test-configs-ssf/ssf-receiver-test-config.json"

    # Run SSF CAEP receiver test plan against SSF CAEP transmitter: test metadata and stream handling
    TESTS="${TESTS} openid-ssf-receiver-caep-test-plan[$PUSH_DELIVERY]:openid-ssf-receiver-stream-create-delete{openid-ssf-transmitter-caep-test-plan[$PUSH_DELIVERY][$STATIC_CLIENT][$SERVER_METADATA_STATIC][$CLIENT_AUTH_CLIENT_SECRET_POST][$SSF_METADATA][$SSF_AUTH_MODE]:openid-ssf-transmitter-metadata,openid-ssf-stream-control-happy-path}${SUITE_DIR}/scripts/test-configs-ssf/ssf-transmitter-test-config.json ${SUITE_DIR}/scripts/test-configs-ssf/ssf-receiver-test-config.json"
    # Run CAEP receiver interop test against CAEP transmitter interop: check SSF CAEP receiver behavior
    TESTS="${TESTS} openid-ssf-receiver-caep-test-plan[$PUSH_DELIVERY]:openid-ssf-receiver-stream-caep-interop{openid-ssf-transmitter-caep-test-plan[$PUSH_DELIVERY][$STATIC_CLIENT][$SERVER_METADATA_STATIC][$CLIENT_AUTH_CLIENT_SECRET_POST][$SSF_METADATA][$SSF_AUTH_MODE]:openid-ssf-transmitter-metadata,openid-ssf-transmitter-stream-caep-interop}${SUITE_DIR}/scripts/test-configs-ssf/ssf-transmitter-test-config.json ${SUITE_DIR}/scripts/test-configs-ssf/ssf-receiver-test-config.json"
}

makeAuthzenTests() {
   TESTS="${TESTS} authzen-pdp-interop-evaluation-test-plan[pdp_auth_type=none][pdp_server_metadata=static] ${SUITE_DIR}/scripts/test-configs-authzen/authzen-cerbos-test-config.json"
   TESTS="${TESTS} authzen-pdp-interop-evaluations-test-plan[pdp_auth_type=none][pdp_server_metadata=static] ${SUITE_DIR}/scripts/test-configs-authzen/authzen-cerbos-test-config.json"
   TESTS="${TESTS} authzen-pdp-interop-subject-search-test-plan[pdp_auth_type=none][pdp_server_metadata=static] ${SUITE_DIR}/scripts/test-configs-authzen/authzen-empowerid-test-config.json"
   TESTS="${TESTS} authzen-pdp-interop-resource-search-test-plan[pdp_auth_type=none][pdp_server_metadata=static] ${SUITE_DIR}/scripts/test-configs-authzen/authzen-empowerid-test-config.json"
   TESTS="${TESTS} authzen-pdp-interop-action-search-test-plan[pdp_auth_type=none][pdp_server_metadata=static] ${SUITE_DIR}/scripts/test-configs-authzen/authzen-empowerid-test-config.json"
}

makeLocalProviderTests() {
    # OIDCC certification tests - server supports discovery, using dcr
    TESTS="${TESTS} oidcc-basic-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-implicit-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-hybrid-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-config-certification-test-plan ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-dynamic-certification-test-plan[response_type=code\ id_token] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-3rdparty-init-login-certification-test-plan[response_type=code\ id_token] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-rp-initiated-logout-certification-test-plan[response_type=code\ id_token][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-frontchannel-rp-initiated-logout-certification-test-plan[response_type=code\ id_token][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-backchannel-rp-initiated-logout-certification-test-plan[response_type=code\ id_token][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-formpost-basic-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-formpost-implicit-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-formpost-hybrid-certification-test-plan[server_metadata=discovery][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # OIDCC
    # client_secret_basic - dynamic client
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_basic][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # client_secret_post - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # client_secret_jwt - dynamic client
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # private_key_jwt - dynamic client
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    #TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=private_key_jwt][response_type=id_token\ token][response_mode=default][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"

    # form post
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_post][response_type=code\ id_token][response_mode=form_post][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"
    TESTS="${TESTS} oidcc-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=form_post][client_registration=dynamic_client] ${SUITE_DIR}/.gitlab-ci/local-provider-oidcc-conformance-config.json"

}

makePanvaTests() {
    TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=ping][client_auth_type=mtls][client_registration=dynamic_client][fapi_ciba_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=poll][client_auth_type=mtls][client_registration=dynamic_client][fapi_ciba_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=ping][client_auth_type=private_key_jwt][client_registration=dynamic_client][fapi_ciba_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=poll][client_auth_type=private_key_jwt][client_registration=dynamic_client][fapi_ciba_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"

    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan-privatejwt.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan-privatejwt.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan-privatejwt.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan-privatejwt.json"

    TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-dpop.json"
    #TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan.json"
    #TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan-plainoauth.json"
    #TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-privatejwt-dpop.json"
    #TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-privatejwt-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan-privatejwt.json"
    TESTS="${TESTS} fapi2-message-signing-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan-privatejwt-plainoauth.json"

    # fapi2-security-profile-final is a subset of fapi2-message-signing-final (which adds
    # signed request/response on top), and the variant pairs are identical
    # (mtls/dpop/openid_connect and private_key_jwt/mtls/plain_oauth), so these are
    # redundant. Commented out to reduce CI time (~42m -> ~32m).
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-dpop.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-privatejwt-dpop.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-privatejwt-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan-privatejwt.json"
    #TESTS="${TESTS} fapi2-security-profile-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan-privatejwt-plainoauth.json"
}

# Resolve conformance-suite-private directory.
# Config files (e.g. authlete-config.json) are bare filenames resolved from CWD,
# so we need to be in the private dir. Detect it automatically if we're not there.
if [ -f "./node-client-setup.sh" ]; then
    # Already in conformance-suite-private
    :
elif [ -f "${SUITE_DIR}/../conformance-suite-private/node-client-setup.sh" ]; then
    cd "${SUITE_DIR}/../conformance-suite-private"
    echo "Changed directory to $(pwd)"
fi

TESTS="${TESTS} --verbose"

# Extract the suite selector and collect any extra args (e.g. --rerun)
SUITE_ARG="${1:-}"
shift || true
EXTRA_ARGS="$*"

if [ -z "$SUITE_ARG" ]; then
    echo "Run all tests"
    makeOidccTest
    makeFapiTest
    makeVcTests
    makeCIBATest
    makeClientTest
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    # ignore that logout tests are untested (Authlete doesn't support the RP initiated logout specs)
    TESTS="${TESTS} --show-untested-test-modules all-except-logout"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
elif [ "$SUITE_ARG" = "--client-tests" ]; then
    echo "Run client tests"
    makeClientTest
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-client.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-client.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules client"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
elif [ "$SUITE_ARG" = "--oidcc-tests" ]; then
    echo "Run OIDCC tests"
    makeOidccTest
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-oidcc.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-oidcc.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules oidcc"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
    TESTS="${TESTS} --no-parallel-for-no-alias" # the jobs without aliases aren't the slowest queue, so avoid overwhelming server early on
elif [ "$SUITE_ARG" = "--fapi-tests" ]; then
    echo "Run FAPI tests"
    makeFapiTest
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-fapi.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-fapi.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules fapi-authlete"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
    TESTS="${TESTS} --no-parallel-for-no-alias" # the jobs without aliases aren't the slowest queue, so avoid overwhelming server early on
elif [ "$SUITE_ARG" = "--ciba-tests" ]; then
    echo "Run ciba tests"
    makeCIBATest
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-ciba.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-ciba.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules ciba"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
    TESTS="${TESTS} --no-parallel" # the authlete authentication device simulator doesn't seem to support parallel authorizations
elif [ "$SUITE_ARG" = "--ekyc-tests" ]; then
    echo "Run eKYC tests"
    makeEkycTests
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-ekyc.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-ekyc.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules ekyc"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
    TESTS="${TESTS} --no-parallel" # both alias queues hit the same authlete instance; serialize to reduce peak concurrent plans
elif [ "$SUITE_ARG" = "--authzen-tests" ]; then
    echo "Run Authzen tests"
    makeAuthzenTests
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-authzen.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-authzen.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules authzen"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
elif [ "$SUITE_ARG" = "--federation-tests" ]; then
    echo "Run federation tests"
    makeFederationTests
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-federation.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-federation.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules federation"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
    TESTS="${TESTS} --no-parallel" # federation tests are fast (~2m); serialize to reduce peak concurrent plans
elif [ "$SUITE_ARG" = "--local-provider-tests" ]; then
    echo "Run local provider tests"
    makeLocalProviderTests
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-local.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-local.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules server-oidc-provider"
    TESTS="${TESTS} --export-dir ."
elif [ "$SUITE_ARG" = "--ssf-tests" ]; then
    echo "Run ssf tests"
    makeSsfTests
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-ssf.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-ssf.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    # TESTS="${TESTS} --show-untested-test-modules ssf"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
elif [ "$SUITE_ARG" = "--vc-tests" ]; then
    echo "Run VP+VCI tests"
    makeVcTests
    EXPECTED_FAILURES_FILE="${SUITE_DIR}/.gitlab-ci/expected-failures-vc.json"
    EXPECTED_SKIPS_FILE="${SUITE_DIR}/.gitlab-ci/expected-skips-vc.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules vc"
    TESTS="${TESTS} --no-parallel" # The authlete tests and the OP-against-RP tests use the same aliases to reduce peak load when CI starts, so we run them in series
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
elif [ "$SUITE_ARG" = "--panva-tests" ]; then
    echo "Run panva tests"
    makePanvaTests
    TESTS="${TESTS} --show-untested-test-modules server-panva"
    TESTS="${TESTS} --export-dir ${SUITE_DIR}"
    TESTS="${TESTS} --no-parallel-for-no-alias" # the jobs without aliases aren't the slowest queue, so avoid overwhelming server early on
elif [ "$SUITE_ARG" = "--security-tests" ]; then
    echo "Run security tests"
    python3 "${SUITE_DIR}/scripts/run-security-tests.py"
    exit $?
else
    echo "Syntax: run-tests.sh [--client-tests|--oidcc-tests|--fapi-tests|--ciba-tests|--local-provider-tests|--panva-tests|--ekyc-tests|--authzen-tests|--federation-tests|--ssf-tests|--vc-tests|--security-tests] [--rerun N|N:M|N,M,...]"
    exit 1
fi

if [ -n "$EXTRA_ARGS" ]; then
    TESTS="${TESTS} ${EXTRA_ARGS}"
fi

# Ctrl+C handling: the launched pipeline is bash → xargs → python3 (→ npm/node
# if client tests are running). By default a SIGINT at the terminal reaches the
# whole foreground process group, but python3's asyncio shutdown can take a few
# seconds and any process that has drifted into its own group (e.g. an npm
# subprocess launched by run-test-plan.py for client tests) won't get the
# signal at all. To guarantee that every descendant is torn down, we trap
# INT/TERM, walk the child tree of the xargs pid recursively via `pgrep -P`,
# send SIGTERM, then force-kill anything still alive a second later.
kill_tree() {
    local pid=$1
    local child
    for child in $(pgrep -P "$pid" 2>/dev/null); do
        kill_tree "$child"
    done
    kill -TERM "$pid" 2>/dev/null || true
}

force_kill_tree() {
    local pid=$1
    local child
    for child in $(pgrep -P "$pid" 2>/dev/null); do
        force_kill_tree "$child"
    done
    kill -KILL "$pid" 2>/dev/null || true
}

abort_handler() {
    trap - INT TERM
    echo
    echo "$(date '+%Y-%m-%d %H:%M:%S'): run-tests.sh interrupted — stopping test run"
    if [ -n "${TEST_RUN_PID:-}" ]; then
        kill_tree "$TEST_RUN_PID"
        sleep 1
        force_kill_tree "$TEST_RUN_PID"
    fi
    exit 130
}
trap abort_handler INT TERM

echo ${TESTS} | xargs -s 100000 ${SUITE_DIR}/scripts/run-test-plan.py &
TEST_RUN_PID=$!
# Disable `set -e` around wait so a non-zero test-runner exit code doesn't
# short-circuit the script before we can record and re-raise it.
set +e
wait "$TEST_RUN_PID"
EXIT_CODE=$?
set -e
exit "$EXIT_CODE"
