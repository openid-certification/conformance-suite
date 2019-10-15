package net.openid.conformance.fapi;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

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
