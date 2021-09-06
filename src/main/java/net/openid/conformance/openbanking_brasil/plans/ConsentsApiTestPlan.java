package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentApiBadScopeTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentApiNegativeTests;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentsApiConsentExpiredTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentsApiConsentStatusIfDeclinedTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentsApiConsentStatusTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentsApiCrossClientTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.ConsentsApiPermissionGroupsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.ResponseMode;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Consents api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CONSENTS_API_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant consents API"
)
public class ConsentsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					ConsentApiTestModule.class,
					//ConsentApiBadScopeTestModule.class,
					ConsentApiNegativeTests.class,
					ConsentsApiPermissionGroupsTestModule.class,
					ConsentsApiCrossClientTestModule.class,
					ConsentsApiConsentStatusTestModule.class,
					ConsentsApiConsentStatusIfDeclinedTestModule.class,
					ConsentsApiConsentExpiredTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}

}
