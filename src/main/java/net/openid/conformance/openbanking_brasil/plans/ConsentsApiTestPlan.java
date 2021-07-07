package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentsApiWideningPermissionsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Consents api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CONSENTS_API_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant consents API",
	testModules = {
		ConsentApiTestModule.class,
		ConsentsApiWideningPermissionsTestModule.class
	})
public class ConsentsApiTestPlan implements TestPlan {
}
