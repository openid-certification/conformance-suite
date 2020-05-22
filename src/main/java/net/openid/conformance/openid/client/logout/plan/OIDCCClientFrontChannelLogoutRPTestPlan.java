package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestFrontChannelLogoutRPInitiated;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutInvalidState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutNoState;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-front-channel-logout-rp",
	displayName = "OpenID Connect Core: Front Channel Logout RP Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientFrontChannelLogoutRPTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {

		final List<Variant> variantSettings = List.of(
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestFrontChannelLogoutRPInitiated.class
				),
				variantSettings
			)
		);
	}
}
