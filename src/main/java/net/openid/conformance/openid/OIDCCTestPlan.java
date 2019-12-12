package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-test-plan",
	displayName = "OpenID Connect Core: Authorization server test (not currently part of certification program)",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		OIDCCAuthCodeReuse.class,
		OIDCCRefreshToken.class,
		OIDCCEnsureRedirectUriInAuthorizationRequest.class,
		OIDCCEnsureRegisteredRedirectUri.class,
		OIDCCEnsureRequestObjectWithRedirectUri.class,
		OIDCCUserInfoGet.class,
		OIDCCUserInfoPostBody.class,
		OIDCCUserInfoPostHeader.class
	}
)
public class OIDCCTestPlan implements TestPlan {

}
