package net.openid.conformance.openinsurance.testplan.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testmodule.resources.v1.OpinResourcesApiTestModule;
import net.openid.conformance.openinsurance.testmodule.resources.v1.OpinResourcesApiTestModuleCorrect404;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance Resources api test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.RESOURCES_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Resources API"
)
public class ResourcesApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					OpinResourcesApiTestModule.class,
					OpinResourcesApiTestModuleCorrect404.class
				),
				List.of(

				)
			)
		);
	}
}
