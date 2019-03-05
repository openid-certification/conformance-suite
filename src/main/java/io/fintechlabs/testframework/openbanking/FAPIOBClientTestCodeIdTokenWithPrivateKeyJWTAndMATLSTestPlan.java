package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ob-client-test-code-id-token-with-private-key-jwt-and-matls-test-plan",
	displayName = "FAPI-OB: client test (code id-token with private_key_jwt and MATLS) Test Plan",
	profile = "FAPI-OB",
	testModuleNames = {
		"fapi-ob-client-test-code-id-token-with-private-key-jwt-and-matls",
		"fapi-ob-client-test-code-id-token-with-private-key-jwt-and-matls-invalid-shash"
	}
)
public class FAPIOBClientTestCodeIdTokenWithPrivateKeyJWTAndMATLSTestPlan implements TestPlan {

}
