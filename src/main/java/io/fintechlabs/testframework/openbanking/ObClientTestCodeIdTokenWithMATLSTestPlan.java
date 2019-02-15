package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "ob-client-test-code-id-token-with-matls-test-plan",
	displayName = "OB: client test (code id-token with MATLS) Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-client-test-code-id-token-with-matls"
	}
)
public class ObClientTestCodeIdTokenWithMATLSTestPlan implements TestPlan {

}
