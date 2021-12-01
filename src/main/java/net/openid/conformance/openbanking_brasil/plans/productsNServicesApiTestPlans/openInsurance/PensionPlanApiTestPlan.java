package net.openid.conformance.openbanking_brasil.plans.productsNServicesApiTestPlans.openInsurance;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.productsNServices.openInsurance.PensionPlanApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;
@PublishTestPlan(
	testPlanName = "ProductsNServices - Pension Plan API test",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.PERSON_PENSION_PLAN_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - Pension Plan API"
)
public class PensionPlanApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PensionPlanApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
