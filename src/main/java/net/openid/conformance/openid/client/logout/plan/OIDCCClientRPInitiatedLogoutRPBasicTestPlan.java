package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.OIDCCClientTest;
import net.openid.conformance.openid.client.OIDCCClientTestClientSecretBasic;
import net.openid.conformance.openid.client.OIDCCClientTestIdTokenSigAlgNone;
import net.openid.conformance.openid.client.OIDCCClientTestIdTokenSignedUsingRS256;
import net.openid.conformance.openid.client.OIDCCClientTestInvalidAudInIdToken;
import net.openid.conformance.openid.client.OIDCCClientTestInvalidIdTokenSignatureWithRS256;
import net.openid.conformance.openid.client.OIDCCClientTestInvalidIssuerInIdToken;
import net.openid.conformance.openid.client.OIDCCClientTestInvalidSubInUserinfoResponse;
import net.openid.conformance.openid.client.OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks;
import net.openid.conformance.openid.client.OIDCCClientTestKidAbsentSingleJwks;
import net.openid.conformance.openid.client.OIDCCClientTestMissingIatInIdToken;
import net.openid.conformance.openid.client.OIDCCClientTestMissingSubInIdToken;
import net.openid.conformance.openid.client.OIDCCClientTestNonceInvalid;
import net.openid.conformance.openid.client.OIDCCClientTestScopeUserInfoClaims;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutInvalidState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutNoState;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-rp-initiated-logout-rp-basic",
	displayName = "OpenID Connect Core: RP Initiated Logout RP Certification Profile Relying Party Tests (Basic)",
	profile = TestPlan.ProfileNames.rplogouttest
)
public class OIDCCClientRPInitiatedLogoutRPBasicTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {

		final List<Variant> variantResponseTypeCode = List.of(
			new Variant(ResponseType.class, "code")
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestRPInitLogout.class,
					OIDCCClientTestRPInitLogoutInvalidState.class,
					OIDCCClientTestRPInitLogoutNoState.class
				),
				variantResponseTypeCode
			)
		);
	}
}
