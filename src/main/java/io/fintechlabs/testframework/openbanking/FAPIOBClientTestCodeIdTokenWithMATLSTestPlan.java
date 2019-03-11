package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ob-client-test-code-id-token-with-matls-test-plan",
	displayName = "FAPI-OB: client test (code id-token with MATLS) Test Plan",
	profile = "FAPI-OB",
	testModuleNames = {
		"fapi-ob-client-test-code-id-token-with-matls",
		"fapi-ob-client-test-code-id-token-with-matls-invalid-shash"
	}
)
public class FAPIOBClientTestCodeIdTokenWithMATLSTestPlan implements TestPlan {

}
