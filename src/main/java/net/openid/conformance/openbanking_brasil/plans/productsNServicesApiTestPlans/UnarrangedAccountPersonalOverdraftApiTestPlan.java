package net.openid.conformance.openbanking_brasil.plans.productsNServicesApiTestPlans;


import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.productsNServices.UnarrangedAccountPersonalOverdraftApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "ProductsNServices - UnarrangedAccountPersonalOverdraft API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.UNARRANGED_ACCOUNT_PERSONAL_OVERDRAFT_API_TEST_PLAN,
	summary = "Structural and logical tests for ProductsNServices - UnarrangedAccountPersonalOverdraft API"
)
public class UnarrangedAccountPersonalOverdraftApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					UnarrangedAccountPersonalOverdraftApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
