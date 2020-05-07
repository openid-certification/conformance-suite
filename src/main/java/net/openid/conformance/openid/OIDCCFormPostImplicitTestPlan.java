package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-formpost-implicit-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Implicit Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCFormPostImplicitTestPlan extends AbstractFormPostTestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(OIDCCImplicitTestPlan.testModulesWithVariants());
	}
}
