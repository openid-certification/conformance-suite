package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AccountApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.FinancingsApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Financings api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.FINANCINGS_API_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Financings API",
	testModules = {
		FinancingsApiTestModule.class
	})
public class FinancingsApiTestPlan implements TestPlan {
}