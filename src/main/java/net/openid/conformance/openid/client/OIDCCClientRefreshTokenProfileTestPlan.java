package net.openid.conformance.openid.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-client-refreshtoken-test-plan",
	displayName = "OpenID Connect Core Client Refresh Token Profile Tests: Relying party refresh token tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		OIDCCClientTestRefreshToken.class,
		OIDCCClientTestRefreshTokenInvalidIssuer.class,
		OIDCCClientTestRefreshTokenInvalidSub.class
	}
)
public class OIDCCClientRefreshTokenProfileTestPlan implements TestPlan {
}
