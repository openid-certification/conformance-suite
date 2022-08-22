package net.openid.conformance.openinsurance.testplan.deprecated;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentApiNegativeTests;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentApiTestModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentsApiConsentExpiredTestModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentsApiConsentStatusIfDeclinedTestModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentsApiConsentStatusTestModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentsApiCrossClientTestModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentsApiDeleteTestModule;
import net.openid.conformance.openinsurance.testmodule.deprecated.consent.OpinConsentsApiPermissionGroupsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Insurance Consents api test v1",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.OPIN_CONSENTS_API_NAME,
	summary = "Structural and logical tests for Open Insurance Brasil Phase 2 - Consents API V1"
)

public class ConsentsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					OpinConsentApiTestModule.class,
					//ConsentApiBadScopeTestModule.class,
					OpinConsentApiNegativeTests.class,
					OpinConsentsApiPermissionGroupsTestModule.class,
					OpinConsentsApiCrossClientTestModule.class,
					OpinConsentsApiConsentStatusTestModule.class,
					OpinConsentsApiConsentStatusIfDeclinedTestModule.class,
					OpinConsentsApiConsentExpiredTestModule.class,
					OpinConsentsApiDeleteTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
