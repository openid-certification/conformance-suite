#!/bin/bash

./scripts/run-test-plan.py 'fapi1-advanced-final-test-plan[client_auth_type=private_key_jwt][fapi_profile=openbanking_brazil][fapi_auth_request_method=by_value][fapi_response_mode=plain_response]' brazil-raidiam-fapi-payments-automated.json
