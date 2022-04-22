package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckPaymentsModule;
import net.openid.conformance.openbanking_brasil.testmodules.pixscheduling.*;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Payments api phase 3 test - Pix Scheduling",
	profile = OBBProfile.OBB_PROFIlE_PHASE3,
	displayName = PlanNames.PAYMENTS_API_PHASE_3_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil scheduled payments API. These tests are designed to validate the payment initation of scheduled Pix payments, including structural integrity and content validation."
)
public class PixSchedulingTestPlan  implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckPaymentsModule.class,
					PixSchedulingDateInFutureConsentsTestModule.class,
					PixSchedulingDateIsTodayConsentsTestModule.class,
					PixScheduledPaymentDateIsInPastConsentsTestModule.class,
					PixScheduledPaymentDateTooFarInFutureConsentsTestModule.class,
					PixScheduledPaymentTestModule.class,
					PixSchedulingPatchConsentsShouldNotBeAuthorisedTestModule.class,
					PixSchedulingPatchShouldNotBeUsedOnAuthorisedConsent.class,
					PixSchedulingPatchHappyPathRevokedByUserTestModule.class,
					PixSchedulingPatchHappyPathRevokedByTppTestModule.class,
					PixSchedulingPatchConsentsIncorrectStatusTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}

}
