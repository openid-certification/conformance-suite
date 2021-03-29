package net.openid.conformance.fapir;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-r-test-plan",
	displayName = "FAPI-R: Authorization server test (not currently part of certification program)",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		// each test module applies to only one client auth method
		CodeIdTokenWithClientSecretJWTAssertion.class,
		CodeIdTokenWithMTLS.class,
		CodeIdTokenWithPKCE.class,
		CodeIdTokenWithPrivateKey.class,

		// applicable to all client authentication methods
		EnsureRedirectUriInAuthorizationRequest.class,
		EnsureRegisteredRedirectUri.class,
		RequirePKCE.class,
		RejectPlainPKCE.class
	}
)
public class FAPI_R_TestPlan implements TestPlan {

}
