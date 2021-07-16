package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Consents api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CONSENTS_API_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant consents API",
	testModules = {
		ConsentApiTestModule.class,
		ConsentsApiWideningPermissionsTestModule.class,
		ConsentsApiCrossClientTestModule.class,
		ConsentsApiConsentStatusTestModule.class
	})
public class ConsentsApiTestPlan implements TestPlan {
}
