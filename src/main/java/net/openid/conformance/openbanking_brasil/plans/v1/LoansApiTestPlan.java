package net.openid.conformance.openbanking_brasil.plans.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v1.LoansApiResourcesTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v1.LoansApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v1.LoansApiWrongPermissionsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Loans api test",
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	displayName = PlanNames.LOANS_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Loans API"
)
public class LoansApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					LoansApiTestModule.class,
					LoansApiWrongPermissionsTestModule.class,
					LoansApiResourcesTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}