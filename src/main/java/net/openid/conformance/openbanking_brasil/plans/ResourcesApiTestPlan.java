package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.ResourcesApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ResourcesApiTestModuleNoResources;
import net.openid.conformance.openbanking_brasil.testmodules.ResourcesApiTestModuleAccount;
import net.openid.conformance.openbanking_brasil.testmodules.ResourcesApiTestModuleCreditCard;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Resources api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.RESOURCES_API_PLAN_NAME,
//	displayName = "Functional tests for resources API (WIP)",
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Resources API",
	testModules = {
		ResourcesApiTestModule.class,
		//ResourcesApiTestModuleNoResources.class, // With the new resource groups, an empty resource request no longer makes any sense.
		//ResourcesApiTestModuleAccount.class, // Not rquired for T0
		//ResourcesApiTestModuleCreditCard.class // Not required for T0
	})
public class ResourcesApiTestPlan implements TestPlan {
}
