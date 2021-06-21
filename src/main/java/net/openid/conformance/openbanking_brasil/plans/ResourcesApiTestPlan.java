package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Resources api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = "Functional tests for resources API (WIP)",
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Resources API",
	testModules = {
		ResourcesApiTestModule.class
	})
public class ResourcesApiTestPlan implements TestPlan {
}
