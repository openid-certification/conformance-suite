package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-formpost-hybrid-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Hybrid Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCFormPostHybridTestPlan extends AbstractFormPostTestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(OIDCCHybridTestPlan.testModulesWithVariants());
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Form Post OP";
	}
}
