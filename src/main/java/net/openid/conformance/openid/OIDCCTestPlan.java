package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-test-plan",
	displayName = "OpenID Connect Core: Authorization server test (not currently part of certification program)",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		OIDCCAuthCodeReuse.class,
		OIDCCDiscoveryEndpointVerification.class,
		OIDCCRefreshToken.class,
		OIDCCEnsureRedirectUriInAuthorizationRequest.class,
		OIDCCEnsureRegisteredRedirectUri.class,
		OIDCCEnsureRequestObjectWithRedirectUri.class,
		OIDCCIdTokenRS256.class,
		OIDCCIdTokenSignature.class,
		OIDCCIdTokenUnsigned.class,
		OIDCCScopeAddress.class,
		OIDCCScopeAll.class,
		OIDCCScopeEmail.class,
		OIDCCScopePhone.class,
		OIDCCScopeProfile.class,
		OIDCCServerTest.class,
		OIDCCUserInfoGet.class,
		OIDCCUserInfoPostBody.class,
		OIDCCUserInfoPostHeader.class
	}
)
public class OIDCCTestPlan implements TestPlan {

}
