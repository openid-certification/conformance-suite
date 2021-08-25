package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.ResourcesApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Resources api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.RESOURCES_API_PLAN_NAME,
//	displayName = "Functional tests for resources API (WIP)",
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Resources API"
)
public class ResourcesApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					ResourcesApiTestModule.class
					//ResourcesApiTestModuleNoResources.class, // With the new resource groups, an empty resource request no longer makes any sense.
					//ResourcesApiTestModuleAccount.class, // Not rquired for T0
					//ResourcesApiTestModuleCreditCard.class // Not required for T0
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
