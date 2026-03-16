package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-formpost-basic-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Basic Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest,
	specFamily = TestPlan.SpecFamilyNames.oidcc
)
public class OIDCCFormPostBasicTestPlan extends AbstractFormPostTestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(new OIDCCBasicTestPlan().testModulesWithVariants());
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("Form Post OP");
	}

}
