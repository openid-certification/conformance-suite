package net.openid.conformance.openinsurance.testplan.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance acceptance and branches abroad api test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.ACCEPTANCE_AND_BRANCHES_ABROAD_PHASE_2_TEST_PLAN,
	summary = "Structural and logical tests for OpenInsurance Brasil-conformant acceptance and branches abroad API"
)

public class AcceptanceAndBranchesAbroadApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(

				),
				List.of(

				)
			)
		);
	}
}
