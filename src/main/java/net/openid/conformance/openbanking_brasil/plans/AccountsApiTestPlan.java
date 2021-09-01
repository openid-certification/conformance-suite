package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AccountApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.AccountsApiReadPermissionsAreRestricted;
import net.openid.conformance.openbanking_brasil.testmodules.AccountsApiUXScreenshots;
import net.openid.conformance.openbanking_brasil.testmodules.AccountsApiWrongPermissionsTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.AccountsApiNegativeTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Account api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.ACCOUNT_API_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Account API"
)
public class AccountsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					AccountApiTestModule.class,
					AccountsApiWrongPermissionsTestModule.class,
					AccountsApiReadPermissionsAreRestricted.class,
					AccountsApiNegativeTestModule.class,
					AccountsApiUXScreenshots.class

				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
