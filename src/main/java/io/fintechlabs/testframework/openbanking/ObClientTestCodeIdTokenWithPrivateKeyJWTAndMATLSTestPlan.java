package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "ob-client-test-code-id-token-with-private-key-jwt-and-matls-test-plan",
	displayName = "OB: client test (code id-token with private_key_jwt and MATLS) Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-client-test-code-id-token-with-private-key-jwt-and-matls"
	}
)
public class ObClientTestCodeIdTokenWithPrivateKeyJWTAndMATLSTestPlan implements TestPlan {

}
