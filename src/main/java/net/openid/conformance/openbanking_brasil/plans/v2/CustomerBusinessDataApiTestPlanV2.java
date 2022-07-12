package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.BusinessEntityWithPersonalPermissionsTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.v2.CustomerBusinessDataApiTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.v2.CustomerBusinessWrongPermissionsTestModuleV2;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Business Customer Data api test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	displayName = PlanNames.CUSTOMER_BUSINESS_DATA_API_PLAN_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Business Customer Data API"
)
public class CustomerBusinessDataApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					BusinessEntityWithPersonalPermissionsTestModule.class,
					CustomerBusinessDataApiTestModuleV2.class,
					CustomerBusinessWrongPermissionsTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
