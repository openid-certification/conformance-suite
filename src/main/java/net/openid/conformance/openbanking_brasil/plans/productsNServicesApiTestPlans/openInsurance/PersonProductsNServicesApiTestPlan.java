package net.openid.conformance.openbanking_brasil.plans.productsNServicesApiTestPlans.openInsurance;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.productsNServices.openInsurance.PersonApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "ProductsNServices - Person API test",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.PRODUCTS_N_SERVICES_PERSON_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - Person API"
)
public class PersonProductsNServicesApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PersonApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
