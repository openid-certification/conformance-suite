package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutInvalidState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutNoState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestSessionManagement;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-rp-session-management-rp",
	displayName = "OpenID Connect Core: Session Management RP Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientSessionManagementRPTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {

		final List<Variant> variantSettings = List.of(
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestSessionManagement.class
				),
				variantSettings
			)
		);
	}
}
