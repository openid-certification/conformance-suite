package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.openbanking_brasil.testmodules.v2.ConsentsApiConsentExpiredTestModuleV2;
import net.openid.conformance.openbanking_brasil.testmodules.v2.ConsentsApiConsentStatusIfDeclinedTestModuleV2;
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
					ConsentsApiTestTransactionDateTime.class,
					ConsentApiBadConsentsTestModule.class,
					ConsentsApiConsentExpiredTestModuleV2.class,
					ConsentsApiConsentStatusIfDeclinedTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
