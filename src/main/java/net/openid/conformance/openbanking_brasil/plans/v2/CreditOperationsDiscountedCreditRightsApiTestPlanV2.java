package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule.v2.CreditOperationsDiscontinuedCreditRightsResourcesApiTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule.v2.CreditOperationsDiscountedApiWrongPermissionsTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule.v2.CreditOperationsDiscountedCreditRightsApiTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.PreFlightCheckV2Module;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Credit operations discounted credit rights api test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2_VERSION2,
	displayName = PlanNames.CREDIT_OPERATIONS_DISCOUNTED_CREDIT_RIGHTS_API_PLAN_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Discounted Credit Rights API"
)
public class CreditOperationsDiscountedCreditRightsApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCheckV2Module.class,
					CreditOperationsDiscountedCreditRightsApiTestModuleV2.class,
					CreditOperationsDiscountedApiWrongPermissionsTestModuleV2.class,
					CreditOperationsDiscontinuedCreditRightsResourcesApiTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
