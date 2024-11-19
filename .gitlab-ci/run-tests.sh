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


    # client FAPI-RW-ID2
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_client_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_client_type=oidc] automated-ob-client-test.json"

    # client FAPI-RW-ID2-OB
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_client_type=oidc] automated-ob-client-test.json"
    TESTS="${TESTS} fapi-rw-id2-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][fapi_auth_request_method=by_value][fapi_response_mode=plain_response][fapi_client_type=oidc] automated-ob-client-test.json"

    # client OpenID Connect Core Client Tests
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_encrypted_response_alg=A128KW][userinfo_encrypted_response_alg=A128GCMKW][request_object_encryption_alg=RSA-OAEP]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[userinfo_encrypted_response_alg=ECDH-ES][userinfo_signed_response_alg=ES256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_signed_response_alg=PS256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_signed_response_alg=ES256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_post][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_signed_response_alg=ES256K][userinfo_signed_response_alg=EdDSA]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_post][response_type=code id_token token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[id_token_encrypted_response_alg=RSA-OAEP][id_token_encrypted_response_enc=A192CBC-HS384][userinfo_encrypted_response_alg=ECDH-ES][userinfo_encrypted_response_enc=A128CBC-HS256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_jwt][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[userinfo_encrypted_response_alg=ECDH-ES][userinfo_encrypted_response_enc=A192CBC-HS384]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_jwt][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=private_key_jwt][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[token_endpoint_auth_signing_alg=ES256]}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=private_key_jwt][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[token_endpoint_auth_signing_alg=ES256]}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=none][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=none][response_type=code\ id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=none][response_type=id_token\ token][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=form_post][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code id_token token][response_mode=form_post][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"

    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][request_type=request_object][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=client_secret_basic][response_type=code\ id_token\ token][response_mode=default][request_type=request_object][client_registration=dynamic_client]{sample-openid-client-nodejs[request_object_encryption_alg=ECDH-ES]}_ automated-oidcc-client-test.json"

    TESTS="${TESTS}  oidcc-client-test-plan[client_auth_type=tls_client_auth][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs[tls_client_auth_subject_dn=cn=test.certification.example.com,o=oidf,l=san ramon,st=ca,c=us](CLIENT_CERT=../../conformance-suite/.gitlab-ci/rp_tests-tls_client_auth.crt)(CLIENT_CERT_KEY=../../conformance-suite/.gitlab-ci/rp_tests-tls_client_auth.key)}_ automated-oidcc-client-test.json"
    TESTS="${TESTS} oidcc-client-test-plan[client_auth_type=self_signed_tls_client_auth][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs(CLIENT_CERT=../../conformance-suite/.gitlab-ci/rp_tests-tls_client_auth.crt)(CLIENT_CERT_KEY=../../conformance-suite/.gitlab-ci/rp_tests-tls_client_auth.key)}_ automated-oidcc-client-test.json"


    # OIDC Core RP refresh token tests
    TESTS="${TESTS} oidcc-client-refreshtoken-test-plan[client_auth_type=client_secret_basic][response_type=code][response_mode=default][request_type=plain_http_request][client_registration=dynamic_client]{sample-openid-client-nodejs}_ automated-oidcc-client-test.json"


    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_profile=openbanking_ksa][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_client_type=oidc] ./ksa-rp-client/fapi-ksa-rp-test-config-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_ksa][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_client_type=oidc] ./ksa-rp-client/fapi-ksa-rp-test-config-mtls.json"
}

makeServerTest() {

    # FAPI2 security profile
    # plain oauth
    TESTS="${TESTS} fapi2-security-profile-id2-test-plan[openid=plain_oauth][client_auth_type=mtls][sender_constrain=mtls][fapi_profile=plain_fapi] authlete-fapi2securityprofile-mtls-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-id2-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=plain_fapi] authlete-fapi2securityprofile-privatekey-plainoauth.json"

    # oidc
    TESTS="${TESTS} fapi2-security-profile-id2-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=plain_fapi] authlete-fapi2securityprofile-privatekey.json"

    # FAPI2 message signing - jar
    TESTS="${TESTS} fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response] authlete-fapi2securityprofile-privatekey-jar.json"

    # FAPI2 message signing - jarm
    TESTS="${TESTS} fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_response_mode=jarm][fapi_profile=plain_fapi] authlete-fapi2securityprofile-privatekey-jarm.json"

    # Brazil
    TESTS="${TESTS} fapi2-security-profile-id2-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][sender_constrain=dpop][fapi_profile=openbanking_brazil] authlete-fapi2securityprofile-brazil-privatekey-dpop.json"
    TESTS="${TESTS} fapi2-security-profile-id2-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][sender_constrain=mtls][fapi_profile=openbanking_brazil] authlete-fapi2securityprofile-brazil-privatekey-plainoauth.json"

    # FAPI2 message signing - jar + connectid
    TESTS="${TESTS} fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response] authlete-fapi2securityprofile-connectid-privatekey.json"

    # FAPI2 message signing - jarm
    TESTS="${TESTS} fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_response_mode=jarm][fapi_profile=openbanking_brazil] authlete-fapi2securityprofile-brazil-privatekey-jarm.json"

    #FAPI2 CBUAE - jar + rar
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-par-without-duplicate-parameters}../conformance-suite/scripts/test-configs-rp-against-op/cbuae-op.json ../conformance-suite/scripts/test-configs-rp-against-op/cbuae-rp.json"
    TESTS="${TESTS} fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=cbuae][fapi_response_mode=plain_response][authorization_request_type=rar] authlete-fapi2securityprofile-privatekey-jar-cbuae.json"

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
    TESTS="${TESTS} fapi1-advanced-final-brazil-dcr-test-plan[fapi_profile=openbanking_brazil][client_auth_type=private_key_jwt][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-dcr.json"

    # Brazil FAPI
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi-brazil-privatekey-encryptedidtoken.json"

    # authlete openbanking uk
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_uk][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-ob-mtls.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_uk][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-ob-privatekey.json"

    # authlete KSA/SAMA openbanking
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_profile=openbanking_ksa][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi1-final-mtls-sama.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_ksa][fapi_response_mode=plain_response][fapi_auth_request_method=pushed] authlete-fapi1-final-privatekey-sama.json"

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
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=jarm][fapi_auth_request_method=pushed] authlete-fapi1-final-privatekey-jarm-encrypted.json"

    # OP tests run against RP tests
    # MTLS
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_client_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # private_key_jwt
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # PAR
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # JARM
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=mtls][fapi_client_type=oidc][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
    # Brazil OB
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"
    # Brazil OPIN
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_auth_request_method=pushed][fapi_profile=openinsurance_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-client-test{fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=openinsurance_brazil][fapi_response_mode=plain_response]:fapi1-advanced-final-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-opin.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-opin.json"

    # Brazil OB DCR accounts (private_key_jwt)
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openbanking_brazil]:fapi1-advanced-final-brazildcr-happy-flow}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-accounts-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-accounts.json"
    # Brazil OB DCR payments (private_key_jwt)
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openbanking_brazil]:fapi1-advanced-final-brazildcr-happy-flow}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-payments-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-payments.json"
    # Brazil OB DCR payments (private_key_jwt). Payment consent request aud as array.
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openbanking_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openbanking_brazil]:fapi1-advanced-final-brazildcr-payment-consent-request-aud-as-array}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-payments-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-payments-1.json"
    # Brazil DCR OPIN (private_key_jwt)
    TESTS="${TESTS} fapi1-advanced-final-client-test-plan[client_auth_type=private_key_jwt][fapi_client_type=oidc][fapi_response_mode=plain_response][fapi_auth_request_method=pushed][fapi_profile=openinsurance_brazil]:fapi1-advanced-final-client-brazildcr-happypath-test{fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_response_mode=plain_response][fapi_profile=openinsurance_brazil]:fapi1-advanced-final-brazildcr-happy-flow}../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-op-test-config-opin-dcr.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-opin-dcr.json"

    # FAPI2 Advanced OP against RP
      # plain_fapi, signed_non_repudiation, plain_response
         # client_auth=private_key, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
         # client_auth=private_key, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config-no-mtls.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"

        # client_auth=mtls, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
        # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config-mtls-client-auth-dpop.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"


    # FAPI2 Advanced OP against RP using EdDSA signing alg
      # plain_fapi, signed_non_repudiation, plain_response
         # client_auth=private_key, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"

         # client_auth=private_key, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-no-mtls.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
         # client_auth=private_key, sender_constrain=dpop, dpop_signing_alg=es256
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-no-mtls-dpop-es256.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"

        # client_auth=mtls, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
        # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-mtls-client-auth-dpop.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"
        # client_auth=mtls, sender_constrain=dpop, dpop_signing_alg=es256
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=signed_non_repudiation][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-eddsa-keytest-config-mtls-client-auth-dpop-es256.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-eddsa-keytest-config.json"


      # connectid, signed_non_repudiation, plain_response, mtls - connectid discovery+happyflow
    # connectid discovery+happyflow
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-au-connectid-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-au-connectid-rp-test-config.json"
    # connectid returning identity claims
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=connectid_au][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-australia-connectid-test-claims-parameter-idtoken-identity-claims}../conformance-suite/scripts/test-configs-rp-against-op/fapi-au-connectid-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-au-connectid-rp-test-config-1.json"

    # plain fapi without openid connect
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][fapi_client_type=plain_oauth][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=plain_oauth][client_auth_type=private_key_jwt][fapi_request_method=signed_non_repudiation][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config-no-openid.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config-no-openid.json"

      # FAPI2 baseline OP against RP
      # These don't use the baseline test plans as currently the run-test-plan.py syntax for selecting individual models doesn't work for these plans, it ends up specifying extra unnecessary variants when scheduling the test
         # client_auth=private_key, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
         # client_auth=private_key, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=private_key_jwt][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=private_key_jwt][fapi_request_method=unsigned][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config-no-mtls.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
        # client_auth=mtls, sender_constrain=mtls
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=unsigned][sender_constrain=mtls][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"
        # client_auth=mtls, sender_constrain=dpop
    TESTS="${TESTS} fapi2-message-signing-id1-client-test-plan[client_auth_type=mtls][fapi_request_method=unsigned][fapi_client_type=oidc][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-client-test-happy-path{fapi2-message-signing-id1-test-plan[openid=openid_connect][client_auth_type=mtls][fapi_request_method=unsigned][sender_constrain=dpop][fapi_profile=plain_fapi][fapi_response_mode=plain_response]:fapi2-security-profile-id2-discovery-end-point-verification,fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-op-test-config-mtls-client-auth-dpop.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-rp-test-config.json"


    # This is the configuration used in the instructions as an example.
    # We keep it here as we want to be sure code changes don't break the example in the instructions, but the downside is there
    # is a chance that users may be using the alias at the same time our tests are running
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_response_mode=plain_response][fapi_auth_request_method=by_value] authlete-fapi-rw-id2-privatekey-for-instructions.json"
    TESTS=""

    # OpenID4VP op-against-rp
    SDJWT="credential_format=sd_jwt_vc"
    MDL="credential_format=iso_mdl"
    SANDNS="client_id_scheme=x509_san_dns"
    SIGNEDREQ="request_method=request_uri_signed"
    DIRECTPOST="response_mode=direct_post"
    DCQL="query_language=dcql"
    PEX="query_language=presentation_exchange"
    CONFIGS="../conformance-suite/scripts/test-configs-rp-against-op"

    # VP ID2
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST]:oid4vp-id2-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id2-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-verifier-happy-flow{oid4vp-id2-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt]:oid4vp-id2-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect-alt.json"

    # VP ID3 - DCQL
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$DCQL]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config-dcql.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config-dcql.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config-dcql.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$DCQL]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config-dcql.json ${CONFIGS}/vp-verifier-test-config-with-redirect-alt.json"

    # VP ID3 - Presentation Exchange
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST][$PEX]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$SDJWT][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-wallet-happy-flow-no-state}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect.json"
    TESTS="${TESTS} oid4vp-id3-verifier-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-verifier-happy-flow{oid4vp-id3-wallet-test-plan[$MDL][$SANDNS][$SIGNEDREQ][$DIRECTPOST.jwt][$PEX]:oid4vp-id3-wallet-happy-flow-with-state-and-redirect}${CONFIGS}/vp-wallet-test-config.json ${CONFIGS}/vp-verifier-test-config-with-redirect-alt.json"
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

    # FAPI CIBA OP against RP
    # MTLS
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=mtls][ciba_mode=poll][fapi_profile=plain_fapi]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-rp-test-config.json"
    # PKJWT
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=poll][fapi_profile=plain_fapi]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-rp-test-config.json"
    # OFBR PKJWT
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=poll][fapi_profile=openbanking_brazil]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-brazil-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=poll][fapi_profile=openbanking_brazil]:fapi-ciba-id1-client-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-ensure-other-scope-order-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config-login_hint.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config-login_hint.json"
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=poll][fapi_profile=openbanking_brazil]:fapi-ciba-id1-client-refresh-token-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-brazil-discovery-end-point-verification,fapi-ciba-id1-ensure-other-scope-order-succeeds}../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config-refresh.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=poll][fapi_profile=openbanking_brazil]:fapi-ciba-id1-client-access-denied-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-brazil-discovery-end-point-verification,fapi-ciba-id1-user-rejects-authentication}../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
    TESTS="${TESTS} fapi-ciba-id1-client-test-plan[client_auth_type=private_key_jwt][ciba_mode=poll][fapi_profile=openbanking_brazil]:fapi-ciba-id1-client-invalid-request-test{fapi-ciba-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][ciba_mode=poll][client_registration=static_client]:fapi-ciba-id1-brazil-discovery-end-point-verification,fapi-ciba-id1-multiple-call-to-token-endpoint}../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-op-test-config.json ../conformance-suite/scripts/test-configs-rp-against-op/fapi-ciba-brazil-rp-test-config.json"
}

makeEkycTests() {
    TESTS="${TESTS} ekyc-test-plan-oidccore[client_auth_type=private_key_jwt][server_metadata=discovery][response_type=code][client_registration=dynamic_client][response_mode=default] authlete-ekyc-privatekey.json"
}

makeFederationTests() {
    TESTS="${TESTS} openid-federation-test-plan[server_metadata=discovery][client_registration=static_client] ../conformance-suite/scripts/test-configs-federation/authlete-federation-fapidev-as.json"
    TESTS="${TESTS} openid-federation-test-plan[server_metadata=discovery][client_registration=static_client] ../conformance-suite/scripts/test-configs-federation/authlete-federation-trust-anchor.json"
    TESTS="${TESTS} openid-federation-test-plan[server_metadata=static][client_registration=static_client] ../conformance-suite/scripts/test-configs-federation/sweden-federation-bankid.json"
    TESTS="${TESTS} openid-federation-test-plan[server_metadata=discovery][client_registration=static_client] ../conformance-suite/scripts/test-configs-federation/sweden-federation-intermediate.json"
    TESTS="${TESTS} openid-federation-test-plan[server_metadata=discovery][client_registration=static_client] ../conformance-suite/scripts/test-configs-federation/sweden-federation-trust-anchor.json"
}

makeSsfTests() {
#    TESTS="${TESTS} openid-ssf-test-plan[server_metadata=discovery][client_registration=static_client] ../conformance-suite/scripts/test-configs-ssf/xxxx.json"
    echo "SSF not implemented yet"
    TESTS="${TESTS}"
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

makePanvaTests() {
    TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=ping][client_auth_type=mtls][client_registration=dynamic_client][fapi_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=poll][client_auth_type=mtls][client_registration=dynamic_client][fapi_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"
    #TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=ping][client_auth_type=private_key_jwt][client_registration=dynamic_client][fapi_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"
    TESTS="${TESTS} fapi-ciba-id1-test-plan[ciba_mode=poll][client_auth_type=private_key_jwt][client_registration=dynamic_client][fapi_profile=plain_fapi] panva-fapi-ciba-id1-test-plan.json"

    TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi-rw-id2-test-plan.json"
    #TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi-rw-id2-test-plan.json"
    #TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi-rw-id2-test-plan.json"
    #TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi-rw-id2-test-plan.json"
    #TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi-rw-id2-test-plan-privatejwt.json"
    #TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi-rw-id2-test-plan-privatejwt.json"
    #TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi-rw-id2-test-plan-privatejwt.json"
    TESTS="${TESTS} fapi-rw-id2-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi-rw-id2-test-plan-privatejwt.json"

    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan-privatejwt.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan-privatejwt.json"
    #TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=jarm] panva-fapi1-advanced-final-test-plan-privatejwt.json"
    TESTS="${TESTS} fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=pushed][fapi_profile=plain_fapi][fapi_response_mode=plain_response] panva-fapi1-advanced-final-test-plan-privatejwt.json"

    TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-dpop.json"
    #TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan.json"
    #TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan-plainoauth.json"
    #TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-privatejwt-dpop.json"
    #TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-message-signing-id1-test-plan-privatejwt-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=openid_connect][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan-privatejwt.json"
    TESTS="${TESTS} fapi2-message-signing-id1-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][fapi_request_method=signed_non_repudiation][fapi_response_mode=jarm][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-message-signing-id1-test-plan-privatejwt-plainoauth.json"

    TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-dpop.json"
    #TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan.json"
    #TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=mtls][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-privatejwt-dpop.json"
    #TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=dpop] panva-fapi2-security-profile-id2-test-plan-privatejwt-dpop-plainoauth.json"
    #TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=openid_connect][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan-privatejwt.json"
    TESTS="${TESTS} fapi2-security-profile-id2-test-plan[client_auth_type=private_key_jwt][fapi_profile=plain_fapi][openid=plain_oauth][sender_constrain=mtls] panva-fapi2-security-profile-id2-test-plan-privatejwt-plainoauth.json"
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
#    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
#    # ignore that logout tests are untested (Authlete doesn't support the RP initiated logout specs)
#    TESTS="${TESTS} --show-untested-test-modules server-authlete"
#    TESTS="${TESTS} --export-dir ../conformance-suite"
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
elif [ "$#" -eq 1 ] && [ "$1" = "--federation-tests" ]; then
    echo "Run federation tests"
    makeFederationTests
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-federation.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-federation.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules federation"
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
elif [ "$#" -eq 1 ] && [ "$1" = "--ssf-tests" ]; then
    echo "Run ssf tests"
    makeSsfTests
    EXPECTED_FAILURES_FILE="../conformance-suite/.gitlab-ci/expected-failures-ssf.json"
    EXPECTED_SKIPS_FILE="../conformance-suite/.gitlab-ci/expected-skips-ssf.json"
    TESTS="${TESTS} --expected-failures-file ${EXPECTED_FAILURES_FILE}"
    TESTS="${TESTS} --expected-skips-file ${EXPECTED_SKIPS_FILE}"
    TESTS="${TESTS} --show-untested-test-modules ssf"
    TESTS="${TESTS} --export-dir ../conformance-suite"
elif [ "$#" -eq 1 ] && [ "$1" = "--panva-tests-only" ]; then
    echo "Run panva tests"
    makePanvaTests
    TESTS="${TESTS} --show-untested-test-modules server-panva"
    TESTS="${TESTS} --export-dir ../conformance-suite"
    TESTS="${TESTS} --no-parallel-for-no-alias" # the jobs without aliases aren't the slowest queue, so avoid overwhelming server early on
else
    echo "Syntax: run-tests.sh [--client-tests-only|--server-tests-only|--ciba-tests-only|--local-provider-tests|--panva-tests-only|--ekyc-tests|--federation-tests|--ssf-tests]"
    exit 1
fi

echo ${TESTS} | xargs -s 100000 ../conformance-suite/scripts/run-test-plan.py
