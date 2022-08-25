package net.openid.conformance.openinsurance.testplan.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testmodule.v1.consents.OpinConsentsApiPermissionGroupsTestModule;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance consents api test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.CONSENTS_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenInsurance Brasil-conformant consents API"
)

public class ConsentsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					OpinConsentsApiPermissionGroupsTestModule.class
				),
				List.of(

				)
			)
		);
	}
}
