package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "ob-client-test-code-with-client-secret-basic-and-matls-test-plan",
	displayName = "OB: client test (code with client_secret_basic authentication and MATLS) Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-client-test-code-with-client-secret-basic-and-matls"
	}
)
public class OBClientTestCodeWithClientSecretBasicAndMATLSTestPlan implements TestPlan {

}
