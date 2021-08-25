package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.LoansApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.LoansApiWrongPermissionsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Loans api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.LOANS_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Loans API"
)
public class LoansApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					LoansApiTestModule.class,
					LoansApiWrongPermissionsTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
