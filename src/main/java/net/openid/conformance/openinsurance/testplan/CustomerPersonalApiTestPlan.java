package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance customer personal api test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.CUSTOMERS_PERSONAL_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenInsurance Brasil-conformant customer personal API"
)

public class CustomerPersonalApiTestPlan implements TestPlan {
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
