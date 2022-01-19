#!/bin/bash

TESTS=()
TESTS+=('Payments api phase 1 test[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]' ./scripts/configs/brazil-raidiam-functional-payments-automated.json)
# These are commented out as these don't fully pass yet, but should be fixable probably
#TESTS+=('Payments api phase 1 test[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_response_mode=jarm]' ./scripts/configs/brazil-raidiam-functional-payments-automated-mtls.json)
#TESTS+=('Consents api test[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]' ./scripts/configs/brazil-raidiam-functional-consents-automated.json)
#TESTS+=('Resources api test[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]' ./scripts/configs/brazil-raidiam-functional-resources-automated.json)
# This is commented out as we've not setup mtls client/config for resources - but it does correctly run the DCR tests
#TESTS+=('Resources api test[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_response_mode=jarm]' ./scripts/configs/brazil-raidiam-functional-resources-automated.json)

TESTS+=(--expected-failures-file ./scripts/configs/payments-ignored-failures.json)

printf "%s\0" "${TESTS[@]}" | xargs -0 ./scripts/run-test-plan.py
