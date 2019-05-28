package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-r-code-id-token-with-private-key-test-plan",
	displayName = "FAPI-R: code id_token with private key Test Plan",
	profile = "FAPI-R",
	testModuleNames = {
		"fapi-r-code-id-token-with-private-key",
		"fapi-r-ensure-redirect-uri-in-authorization-request",
		"fapi-r-ensure-redirect-uri-is-registered",
		"fapi-r-require-pkce",
		"fapi-r-reject-plain-pkce"
	}
)
public class FAPI_R_CodeIdTokenWithPrivateKeyTestPlan implements TestPlan {

}
