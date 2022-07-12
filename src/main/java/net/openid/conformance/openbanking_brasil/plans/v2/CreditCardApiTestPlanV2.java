package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v2.*;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Credit card api test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2_VERSION2,
	displayName = PlanNames.CREDIT_CARDS_API_PLAN_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Credit Cards API"
)
public class CreditCardApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					CreditCardApiTransactionCurrentTestModuleV2.class,
					CreditCardApiTestModuleV2.class,
					CreditCardApiWrongPermissionsTestModuleV2.class,
					CreditCardApiPageSizeTestModuleV2.class,
					CreditCardApiPageSizeTooLargeTestModuleV2.class,
					CreditCardApiMaxPageSizePagingTestModuleV2.class,
					CreditCardApiResourcesTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
