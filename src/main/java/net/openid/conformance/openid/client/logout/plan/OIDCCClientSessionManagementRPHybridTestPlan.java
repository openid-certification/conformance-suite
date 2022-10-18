package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestSessionManagement;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-rp-session-management-rp-hybrid",
	displayName = "OpenID Connect Core: Session Management RP Certification Profile Relying Party Tests (Hybrid)",
	profile = TestPlan.ProfileNames.rplogouttest
)
public class OIDCCClientSessionManagementRPHybridTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {

		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestSessionManagement.class
				),
				List.of(new Variant(ResponseType.class, "code id_token"))
			),
			new ModuleListEntry(
				List.of(
					OIDCCClientTestSessionManagement.class
				),
				List.of(new Variant(ResponseType.class, "code token"))
			),
			new ModuleListEntry(
				List.of(
					OIDCCClientTestSessionManagement.class
				),
				List.of(new Variant(ResponseType.class, "code id_token token"))
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Session RP";
	}

}
