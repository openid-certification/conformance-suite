package net.openid.conformance.openbanking_brasil.plans.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.testmodules.v1.CreditOperationsAdvancesApiResourcesTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.testmodules.v1.CreditOperationsAdvancesApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.testmodules.v1.CreditOperationsAdvancesApiWrongPermissionsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Unarranged overdraft api test",
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	displayName = PlanNames.CREDIT_OPERATIONS_ADVANCES_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Unarranged Overdraft API"
)
public class CreditOperationsAdvancesApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					CreditOperationsAdvancesApiTestModule.class,
					CreditOperationsAdvancesApiWrongPermissionsTestModule.class,
					CreditOperationsAdvancesApiResourcesTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
