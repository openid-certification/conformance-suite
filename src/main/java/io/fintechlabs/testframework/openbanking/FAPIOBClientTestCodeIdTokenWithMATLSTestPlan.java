package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ob-client-test-code-id-token-with-matls-test-plan",
	displayName = "FAPI-OB: client test (code id-token with MATLS) Test Plan",
	profile = "FAPI-OB",
	testModuleNames = {
		"fapi-ob-client-test-code-id-token-with-matls",
		"fapi-ob-client-test-code-id-token-with-matls-missing-athash",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-shash",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-chash",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-nonce",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-iss",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-aud",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-secondary-aud",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-openbanking-intent-id",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-signature",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-null-alg",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-alternate-alg",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-missing-exp",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-expired-exp"
	}
)
public class FAPIOBClientTestCodeIdTokenWithMATLSTestPlan implements TestPlan {

}
