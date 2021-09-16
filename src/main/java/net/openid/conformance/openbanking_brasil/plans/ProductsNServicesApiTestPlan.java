package net.openid.conformance.openbanking_brasil.plans;


import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.productsNServices.ProductsNServicesApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "ProductsNServices api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.PRODUCTS_N_SERVICES_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant ProductsNServices API"
)
public class ProductsNServicesApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					ProductsNServicesApiTestModule.class

				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
