package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.CreditOperationsAdvancesApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Unarranged overdraft api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CREDIT_OPERATIONS_ADVANCES_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Unarranged Overdraft API",
	testModules = {
		CreditOperationsAdvancesApiTestModule.class
	})
public class CreditOperationsAdvancesApiTestPlan implements TestPlan {
}
