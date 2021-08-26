package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.CustomerPersonalDataApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.CustomerPersonalWrongPermissionsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Personal Customer Data api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CUSTOMER_PERSONAL_DATA_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Personal Customer Data API"
)
public class CustomerPersonalDataApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CustomerPersonalDataApiTestModule.class,
					CustomerPersonalWrongPermissionsTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
