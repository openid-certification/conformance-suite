package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AccountApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.LoansApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Loans api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.LOANS_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Loans API",
	testModules = {
		LoansApiTestModule.class
	})
public class LoansApiTestPlan implements TestPlan {
}
