package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule.CreditOperationsDiscountedApiWrongPermissionsTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule.CreditOperationsDiscountedCreditRightsApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Credit operations discounted credit rights api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CREDIT_OPERATIONS_DISCOUNTED_CREDIT_RIGHTS_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Discounted Credit Rights API"
)
public class CreditOperationsDiscountedCreditRightsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CreditOperationsDiscountedCreditRightsApiTestModule.class,
					CreditOperationsDiscountedApiWrongPermissionsTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
