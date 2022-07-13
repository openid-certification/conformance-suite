package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v2.LoansApiResourcesTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v2.LoansApiTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v2.LoansApiWrongPermissionsTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.PreFlightCheckV2Module;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Loans api test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2_VERSION2,
	displayName = PlanNames.LOANS_API_PLAN_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Loans API"
)
public class LoansApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCheckV2Module.class,
					LoansApiTestModuleV2.class,
					LoansApiWrongPermissionsTestModuleV2.class,
					LoansApiResourcesTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
