#!/bin/sh

TESTS=""

# The non-DCR tests currently only work when run locally as the redirect urls for our review apps aren't registered
# TESTS="$TESTS fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=by_value][fapi_response_mode=plain_response] ../conformance-suite/scripts/test-configs-brazil/brazil-raidiam-fapi-payments-automated.json"

TESTS="$TESTS fapi1-advanced-final-brazil-dcr-test-plan[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response] ../conformance-suite/scripts/test-configs-brazil/brazil-raidiam-fapi-dcr-payments-automated.json"

../conformance-suite/scripts/run-test-plan.py $TESTS
