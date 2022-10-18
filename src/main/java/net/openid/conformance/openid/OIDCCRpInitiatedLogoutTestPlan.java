package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-rp-initiated-logout-certification-test-plan",
	displayName = "OpenID Connect Core: Rp Initiated Logout Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCRpInitiatedLogoutTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match 'RP-Initiated Logout OP' as defined here:
		// https://openid.net/certification/logout_op_testing/

		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCRpInitiatedLogoutDiscoveryEndpointVerification.class
				),
				List.of(
					new Variant(ServerMetadata.class, "discovery")
				)
			),
			new ModuleListEntry(
				List.of(
					OIDCCRpInitiatedLogout.class,
					OIDCCRpInitiatedLogoutBadLogoutRedirectUri.class,
					OIDCCRpInitiatedLogoutModifiedIdTokenHint.class,
					OIDCCRpInitiatedLogoutNoIdTokenHint.class,
					OIDCCRpInitiatedLogoutNoParams.class,
					OIDCCRpInitiatedLogoutNoPostLogoutRedirectUri.class,
					OIDCCRpInitiatedLogoutNoState.class,
					OIDCCRpInitiatedLogoutOnlyState.class,
					OIDCCRpInitiatedLogoutQueryAddedToLogoutRedirectUri.class,
					OIDCCRpInitiatedLogoutBadIdTokenHint.class
				),
				List.of(
					new Variant(ServerMetadata.class, "discovery"),
					new Variant(ClientAuthType.class, "client_secret_basic"),
					new Variant(ResponseMode.class, "default")
				)
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "RP-Initiated OP";
	}

}
