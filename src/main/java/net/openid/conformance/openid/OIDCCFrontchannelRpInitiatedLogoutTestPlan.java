package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-frontchannel-rp-initiated-logout-certification-test-plan",
	displayName = "OpenID Connect Core: Frontchannel Rp Initiated Logout Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCFrontchannelRpInitiatedLogoutTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match 'Front-Channel Logout OP' as defined here:
		// https://openid.net/certification/logout_op_testing/

		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCFrontchannelLogoutDiscoveryEndpointVerification.class
				),
				List.of(
					new Variant(ServerMetadata.class, "discovery")
				)
			),
			new ModuleListEntry(
				List.of(
					OIDCCFrontChannelRpInitiatedLogout.class
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
		return "Front-Channel OP";
	}

}
