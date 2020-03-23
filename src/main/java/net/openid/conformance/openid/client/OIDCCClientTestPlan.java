package net.openid.conformance.openid.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-client-test-plan",
	displayName = "OpenID Connect Core Client Tests: Relying party tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		OIDCCClientTest.class,
		OIDCCClientTestNonceInvalid.class,
		OIDCCClientTestClientSecretBasic.class,
		OIDCCClientTestScopeUserInfoClaims.class,
		OIDCCClientTestKidAbsentSingleJwks.class,
		OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks.class,
		OIDCCClientTestMissingIatInIdToken.class,
		OIDCCClientTestMissingAudInIdToken.class,
		OIDCCClientTestInvalidAudInIdToken.class,
		OIDCCClientTestIdTokenSigAlgNone.class,
		OIDCCClientTestIdTokenSignedUsingRS256.class,
		OIDCCClientTestMissingSubInIdToken.class,
		OIDCCClientTestInvalidIdTokenSignatureWithRS256.class,
		OIDCCClientTestInvalidIssuerInIdToken.class,
		OIDCCClientTestInvalidSubInUserinfoResponse.class,
		OIDCCClientTestUserinfoBearerHeader.class,
		OIDCCClientTestNonce.class,
		OIDCCClientTestMissingCHashInIdToken.class,
		OIDCCClientTestInvalidCHashInIdToken.class,
		OIDCCClientTestInvalidAtHashInIdToken.class,
		OIDCCClientTestMissingAtHashInIdToken.class,
		OIDCCClientTestFormPostError.class,
		//TODO refresh token tests should/will be moved to a "profile" when testing profiles are implemented.
		// or they may be optional when "optional tests" are implemented
		OIDCCClientTestRefreshToken.class,
		OIDCCClientTestRefreshTokenInvalidIssuer.class,
		OIDCCClientTestRefreshTokenInvalidSub.class
		//These tests are probably too strict for RPs so commenting them out for now.
		//See https://gitlab.com/openid/conformance-suite/-/merge_requests/880#note_309383505 for more details
		//OIDCCClientTestRefreshTokenInvalidAud.class,
		//OIDCCClientTestRefreshTokenInvalidAzp.class
	}
)
public class OIDCCClientTestPlan implements TestPlan {
}
