package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules.v2.FinancingApiWrongPermissionsTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules.v2.FinancingsApiResourcesTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules.v2.FinancingsApiTestModuleV2;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Financings api test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	displayName = PlanNames.FINANCINGS_API_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Financings API"
)
public class FinancingsApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					FinancingsApiTestModuleV2.class,
					FinancingApiWrongPermissionsTestModuleV2.class,
					FinancingsApiResourcesTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
