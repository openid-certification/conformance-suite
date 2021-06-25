package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.CreditOperationsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Credit Operations api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CREDIT_OPERATIONS_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Credit Operations API",
	testModules = {
		CreditOperationsTestModule.class
	})
public class CreditOperationsApiTestPlan implements TestPlan {
}
