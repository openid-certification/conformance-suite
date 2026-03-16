package net.openid.conformance.openid.client;

import net.openid.conformance.openid.AbstractFormPostTestPlan;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-formpost-hybrid-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Hybrid Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest,
	specFamily = TestPlan.SpecFamilyNames.oidcc
)
public class OIDCCClientFormPostHybridTestPlan extends AbstractFormPostTestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(new OIDCCClientHybridTestPlan().testModulesWithVariants());
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("Form Post RP");
	}

}
