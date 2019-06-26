package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-r-code-id-token-with-pkce-test-plan",
	displayName = "FAPI-R: code id_token with PKCE Test Plan",
	profile = "FAPI-R",
	testModules = {
		CodeIdTokenWithPKCE.class,
		EnsureRedirectUriInAuthorizationRequest.class,
		EnsureRegisteredRedirectUri.class,
		RequirePKCE.class,
		RejectPlainPKCE.class
	}
)
public class FAPI_R_CodeIdTokenWithPKCETestPlan implements TestPlan {

}
