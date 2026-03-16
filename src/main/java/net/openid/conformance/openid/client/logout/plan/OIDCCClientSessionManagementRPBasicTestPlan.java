package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestSessionManagement;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-rp-session-management-rp-basic",
	displayName = "OpenID Connect Core: Session Management RP Certification Profile Relying Party Tests (Basic)",
	profile = TestPlan.ProfileNames.rplogouttest,
	specFamily = TestPlan.SpecFamilyNames.oidccSessionManagement
)
public class OIDCCClientSessionManagementRPBasicTestPlan implements TestPlan {
	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

		final List<Variant> variantResponseTypeCode = List.of(
			new Variant(ResponseType.class, "code")
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestSessionManagement.class
				),
				variantResponseTypeCode
			)
		);
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("Session RP");
	}

}
