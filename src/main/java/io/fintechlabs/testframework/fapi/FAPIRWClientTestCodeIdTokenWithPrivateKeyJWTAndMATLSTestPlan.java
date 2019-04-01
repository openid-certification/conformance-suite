package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-test-plan",
	displayName = "FAPI-RW: client test (code id-token with private_key_jwt and MATLS) Test Plan",
	profile = "FAPI-rw",
	testModuleNames = {
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-shash",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-chash",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-nonce",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-iss",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-aud",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-secondary-aud",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-signature",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-null-alg",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-alternate-alg",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-missing-exp",
		"fapi-rw-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-expired-exp"
	}
)
public class FAPIRWClientTestCodeIdTokenWithPrivateKeyJWTAndMATLSTestPlan implements TestPlan {

}
