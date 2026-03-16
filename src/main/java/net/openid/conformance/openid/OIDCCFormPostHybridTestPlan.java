package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-formpost-hybrid-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Hybrid Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest,
	specFamily = TestPlan.SpecFamilyNames.oidcc
)
public class OIDCCFormPostHybridTestPlan extends AbstractFormPostTestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(new OIDCCHybridTestPlan().testModulesWithVariants());
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("Form Post OP");
	}
}
