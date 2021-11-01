#!/bin/sh

./scripts/run-test-plan.py 'Payments api phase 1 test[client_auth_type=private_key_jwt][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]' ./scripts/configs/brazil-raidiam-functional-payments-automated.json
