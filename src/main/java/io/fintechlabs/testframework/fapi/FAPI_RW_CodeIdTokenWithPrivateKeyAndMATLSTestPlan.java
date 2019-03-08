package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "fapi-rw-code-id-token-with-private-key-and-matls-test-plan",
	displayName = "FAPI-RW: code id_token with private key and matls Test Plan",
	profile = "FAPI-RW",
	testModuleNames = {
		"fapi-rw-ensure-client-id-in-token-endpoint-with-private-key-and-matls",
		"fapi-rw-ensure-request-object-without-exp-fails-with-private-key-and-matls",
		"fapi-rw-ensure-request-object-without-scope-fails-with-private-key-and-matls",
		"fapi-rw-ensure-request-object-without-state-fails-with-private-key-and-matls",
	}
)
public class FAPI_RW_CodeIdTokenWithPrivateKeyAndMATLSTestPlan implements TestPlan {

}
