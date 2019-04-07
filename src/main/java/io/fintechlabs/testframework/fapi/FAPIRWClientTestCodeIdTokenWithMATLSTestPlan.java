package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-client-test-code-id-token-with-matls-test-plan",
	displayName = "FAPI-RW: client test (code id-token with MATLS) Test Plan",
	profile = "FAPI-RW",
	testModuleNames = {
		"fapi-rw-client-test-code-id-token-with-matls",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-shash",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-chash",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-nonce",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-iss",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-aud",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-secondary-aud",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-signature",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-null-alg",
		"fapi-rw-client-test-code-id-token-with-matls-invalid-alternate-alg"
	}
)
public class FAPIRWClientTestCodeIdTokenWithMATLSTestPlan implements TestPlan {

}
