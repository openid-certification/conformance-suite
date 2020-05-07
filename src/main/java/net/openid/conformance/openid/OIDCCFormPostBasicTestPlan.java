package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-formpost-basic-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Basic Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCFormPostBasicTestPlan extends AbstractFormPostTestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(OIDCCBasicTestPlan.testModulesWithVariants());
	}
}
