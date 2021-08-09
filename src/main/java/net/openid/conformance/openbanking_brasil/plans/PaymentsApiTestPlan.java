package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Payments api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.PAYMENTS_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant payments API",
	testModules = {
		PaymentsApiTestModule.class
	})
public class PaymentsApiTestPlan implements TestPlan {
}
