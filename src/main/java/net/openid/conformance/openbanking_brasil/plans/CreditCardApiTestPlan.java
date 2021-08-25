package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.CreditCardApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.CreditCardApiWrongPermissionsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Credit card api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CREDIT_CARDS_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Credit Cards API"
)
public class CreditCardApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CreditCardApiTestModule.class,
					CreditCardApiWrongPermissionsTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
