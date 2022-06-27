package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountApiBookingDateTestV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountApiTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiMaxPageSizePagingTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiNegativeTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiPageSizeTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiPageSizeTooLargeTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiReadPermissionsAreRestrictedV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiTransactionsCurrentTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiUXScreenshotsV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsApiWrongPermissionsTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.accounts.AccountsResourcesApiTestModuleV2;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Accounts API Test "+ PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	displayName = PlanNames.ACCOUNT_API_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Account API."
)
public class AccountsApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					AccountsApiTransactionsCurrentTestModuleV2.class,
					AccountsApiResourcesMultipleConsentsTestModule.class,
					AccountApiTestModuleV2.class,
					AccountsApiWrongPermissionsTestModuleV2.class,
					AccountsApiReadPermissionsAreRestrictedV2.class,
					AccountsApiNegativeTestModuleV2.class,
					AccountsApiUXScreenshotsV2.class,
					AccountsApiPageSizeTestModuleV2.class,
					AccountsApiPageSizeTooLargeTestModuleV2.class,
					AccountsApiMaxPageSizePagingTestModuleV2.class,
					AccountApiBookingDateTestV2.class,
					AccountsResourcesApiTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
