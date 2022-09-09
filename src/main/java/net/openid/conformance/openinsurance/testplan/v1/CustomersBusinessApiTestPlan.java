package net.openid.conformance.openinsurance.testplan.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.openinsurance.testmodule.customers.v1.*;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance customer business api test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.CUSTOMERS_BUSINESS_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenInsurance Brasil-conformant customer business API"
)

public class CustomersBusinessApiTestPlan implements TestPlan {
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
