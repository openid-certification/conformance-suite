package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ob-client-test-code-with-client-secret-basic-and-matls-test-plan",
	displayName = "FAPI-OB: client test (code with client_secret_basic authentication and MATLS) Test Plan",
	profile = "FAPI-OB",
	testModuleNames = {
		"fapi-ob-client-test-code-with-client-secret-basic-and-matls"
	}
)
public class FAPIOBClientTestCodeWithClientSecretBasicAndMATLSTestPlan implements TestPlan {

}
