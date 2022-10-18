package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-session-management-certification-test-plan",
	displayName = "OpenID Connect Core: Session Management Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCSessionManagementTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match 'Session Management OP' as defined here:
		// https://openid.net/certification/logout_op_testing/

		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCSessionManagementDiscoveryEndpointVerification.class
				),
				List.of(
					new Variant(ServerMetadata.class, "discovery")
				)
			),
			new ModuleListEntry(
				List.of(
					OIDCCSessionManagementRpInitiatedLogout.class
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
		return "Session OP";
	}

}
