package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-client-test-code-with-client-secret-basic-and-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2-OB: client test (code with client_secret_basic authentication and mtls holder of key) Test Plan",
	profile = "FAPI-RW-ID2-OB",
	testModuleNames = {
		"fapi-rw-id2-ob-client-test-code-with-client-secret-basic-and-mtls-holder-of-key"
	}
)
public class FAPIOBClientTestCodeWithClientSecretBasicAndMATLSTestPlan implements TestPlan {

}
