package net.openid.conformance.openinsurance.testplan;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openinsurance.testmodule.customers.OpinCustomersBusinessEntityWithPersonalPermissionsTestModuleV1;
import net.openid.conformance.openinsurance.testmodule.customers.OpinCustomersBusinessDataApiTestModuleV1;
import net.openid.conformance.openinsurance.testmodule.customers.OpinCustomersBusinessWrongPermissionsTestModuleV1;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance Customers Business Data api test " + PlanNames.LATEST_VERSION_OPIN_PHASE2,
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.CUSTOMER_BUSINESS_DATA_API_PLAN_NAME_PHASE2,
	summary = "Structural and logical tests for Open Insurance Brasil Phase 2 - Customers Business Data API V1"
)
public class OpinCustomersBusinessDataApiTestPlanV1 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					OpinCustomersBusinessEntityWithPersonalPermissionsTestModuleV1.class,
					OpinCustomersBusinessDataApiTestModuleV1.class,
					OpinCustomersBusinessWrongPermissionsTestModuleV1.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
