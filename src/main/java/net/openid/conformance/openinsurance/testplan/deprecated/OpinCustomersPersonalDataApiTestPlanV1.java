package net.openid.conformance.openinsurance.testplan.deprecated;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.customers.OpinCustomersPersonalDataApiTestModuleV1;
import net.openid.conformance.openinsurance.testmodule.deprecated.customers.OpinCustomersPersonalWrongPermissionsTestModuleV1;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

//@PublishTestPlan(
//	testPlanName = "Open Insurance Customers Personal Data api test " + PlanNames.LATEST_VERSION_OPIN_PHASE2,
//	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
//	displayName = PlanNames.CUSTOMER_PERSONAL_DATA_API_PLAN_NAME_PHASE2,
//	summary = "Structural and logical tests for Open Insurance Brasil Phase 2 - Customers Personal Data API V1"
//)
public class OpinCustomersPersonalDataApiTestPlanV1 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					OpinCustomersPersonalDataApiTestModuleV1.class,
					OpinCustomersPersonalWrongPermissionsTestModuleV1.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
