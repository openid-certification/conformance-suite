package net.openid.conformance.openinsurance.testplan.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance structural tests testplan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.STRUCTURAL_TEST_PLAN,
	summary = "Structural and logical tests for OpenInsurance Brasil-conformant consents API"
)

public class StructuralTestPlan implements TestPlan{
	public static List<TestPlan.ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new TestPlan.ModuleListEntry(
				List.of(

				),
				List.of(

				)
			)
		);
	}
}
