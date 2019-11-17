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
		OIDCCClientTestInvalidCHashInIdToken.class
	}
)
public class OIDCCClientTestPlan implements TestPlan {
}
