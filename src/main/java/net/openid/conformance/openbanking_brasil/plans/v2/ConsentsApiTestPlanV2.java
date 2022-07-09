package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.*;

import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiConsentStatusIfDeclinedTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiConsentExpiredTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiRevokedAspspTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentApiBadConsentsTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentApiNegativeTestsV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentApiTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiConsentStatusTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiCrossClientTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiDeleteTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiPermissionGroupsTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.consents.ConsentsApiTestTransactionDateTimeV2;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Consents api test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	displayName = PlanNames.CONSENTS_API_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant consents API"
)

public class ConsentsApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					ConsentsApiTestTransactionDateTimeV2.class,
					ConsentApiBadConsentsTestModuleV2.class,
					ConsentApiTestModuleV2.class,
					ConsentApiNegativeTestsV2.class,
					ConsentsApiPermissionGroupsTestModuleV2.class,
					ConsentsApiCrossClientTestModuleV2.class,
					ConsentsApiConsentStatusTestModuleV2.class,
					ConsentsApiConsentStatusIfDeclinedTestModuleV2.class,
					ConsentsApiConsentExpiredTestModuleV2.class,
					ConsentsApiDeleteTestModuleV2.class,
					ConsentsApiRevokedAspspTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
