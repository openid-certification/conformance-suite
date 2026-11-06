package net.openid.conformance.poppy.company;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "poppy-company-test-plan",
	shortName = "Company happy flow",
	certifiable = false,
	alpha = true,
	displayName = "Poppy 0.4 Company test plan (alpha, not part of a certification program)",
	profile = TestPlan.ProfileNames.poppyCompanyTest,
	specFamily = TestPlan.SpecFamilyNames.poppy,
	testModules = {
		PoppyCompanyHappyFlow.class
	}
)
public class PoppyCompanyTestPlan implements TestPlan {
}
