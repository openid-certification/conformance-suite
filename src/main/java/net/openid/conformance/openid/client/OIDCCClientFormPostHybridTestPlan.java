package net.openid.conformance.openid.client;

import net.openid.conformance.openid.AbstractFormPostTestPlan;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-formpost-hybrid-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Hybrid Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientFormPostHybridTestPlan extends AbstractFormPostTestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(OIDCCClientHybridTestPlan.testModulesWithVariants());
	}
}
