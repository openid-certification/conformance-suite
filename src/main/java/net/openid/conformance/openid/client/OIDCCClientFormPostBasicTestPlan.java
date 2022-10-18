package net.openid.conformance.openid.client;

import net.openid.conformance.openid.AbstractFormPostTestPlan;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-formpost-basic-certification-test-plan",
	displayName = "OpenID Connect Core: Form Post Basic Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientFormPostBasicTestPlan extends AbstractFormPostTestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return changeResponseTypeToFormPost(OIDCCClientBasicTestPlan.testModulesWithVariants());
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Form Post RP";
	}

}
