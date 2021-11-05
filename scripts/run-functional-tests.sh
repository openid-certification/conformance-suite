#!/bin/bash

TESTS=()
TESTS+=('Payments api phase 1 test[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]' ./scripts/configs/brazil-raidiam-functional-payments-automated.json)
TESTS+=('Payments api phase 1 test[client_auth_type=mtls][fapi_auth_request_method=pushed][fapi_response_mode=jarm]' ./scripts/configs/brazil-raidiam-functional-payments-automated-mtls.json)
TESTS+=('Consents api test[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]' ./scripts/configs/brazil-raidiam-functional-consents-automated.json)

printf "%s\0" "${TESTS[@]}" | xargs -0 ./scripts/run-test-plan.py
