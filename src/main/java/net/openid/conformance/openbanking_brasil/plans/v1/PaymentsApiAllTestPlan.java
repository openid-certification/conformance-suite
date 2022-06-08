package net.openid.conformance.openbanking_brasil.plans.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentsApiBadPaymentSignatureFails;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentsApiFapiTesting;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.openbanking_brasil.testmodules.pixscheduling.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.PaymentsConsentsApiInvalidTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.PaymentsConsumedConsentsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;


@PublishTestPlan(
	testPlanName = "PIX Functional Complete",
	profile = OBBProfile.DEV_ONLY,
	displayName = PlanNames.PAYMENTS_API_ALL_TEST_PLAN,
	summary = "All PIX tests"
)
public class PaymentsApiAllTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PaymentsConsentsApiEnforceQRDNTestModule.class,
					PaymentsConsentsApiEnforceQRESTestModule.class,
					PaymentsConsentsApiEnforceQRESNWithEmailAddressTestModule.class,
					PaymentsConsentsApiEnforceQRESWithPhoneNumberTestModule.class,
					PaymentsConsentsApiEnforceQRESWrongAmountTestModule.class,
					PaymentsConsentsApiEnforceQRESWrongProxyTestModule.class,
					PaymentsConsentsApiEnforceQRDNWithQRESCodeTestModule.class,
					PaymentsApiQRESMismatchConsentPaymentTestModule.class,
					PaymentsConsentsApiQRDNHappyTestModule.class,
					PaymentsConsentsApiQresTransactionIdentifierTestModule.class,
					// the below are also in the phase 1 test plan
					PaymentsApiTestModule.class,
					PaymentsApiNegativeTestModule.class,
					PaymentApiNoDebtorProvidedTestModule.class, //TODO stopped here
					PaymentsConsentsApiTestModule.class,
					PaymentsConsumedConsentsTestModule.class,
					PaymentsConsentsApiEnforceMANUTestModule.class,
					PaymentsConsentsApiMANUPixResponseTestModule.class,
					PaymentsConsentsApiEnforceDICTTestModule.class,
					PaymentsConsentsApiDICTPixResponseTestModule.class,
					PaymentsConsentsApiINICPixResponseTestModule.class,
					PaymentsConsentsApiEmailAddressProxyTestModule.class,
					PaymentsApiWrongEmailAddressProxyTestModule.class,
					PaymentsApiRealEmailAddressWrongCreditorProxyTestModule.class,
					PaymentApiNoDebtorProvidedRealCreditorTestModule.class,
					PaymentsConsentsApiPhoneNumberProxyTestModule.class,
					PaymentsApiIncorrectCPFProxyTestModule.class,
					PaymentsConsentsApiBadPaymentTypeTestModule.class,
					PaymentsConsentsApiDateTestModule.class,
					PaymentsConsentsInvalidPersonTypeTestModule.class,
					PaymentsConsentsReuseJtiTestModule.class,
					PaymentsConsentsJsonAcceptHeaderJwtReturnedTestModule.class,
					PaymentsConsentsReuseIdempotencyKeyTestModule.class,
					PaymentsApiDcrHappyFlowTestModule.class,
					PaymentsApiDcrTestModuleUnauthorizedClient.class,
					PaymentsApiDcrTestModuleAttemptClientTakeover.class,
					PaymentsApiDcrSubjectDn.class,
					PaymentsApiBadPaymentSignatureFails.class,
					PaymentsConsentsApiForceCheckBadSignatureTest.class,
					PaymentsApiInvalidCnpjTestModule.class,
					PaymentsConsentsApiInvalidTestModule.class,
					// PIX scheduling below
					PixSchedulingDateInFutureConsentsTestModule.class,
					PixSchedulingDateIsTodayConsentsTestModule.class,
					PixScheduledPaymentDateIsInPastConsentsTestModule.class,
					PixScheduledPaymentDateTooFarInFutureConsentsTestModule.class,
					PixScheduledPaymentTestModule.class,
					PixSchedulingPatchConsentsShouldNotBeAuthorisedTestModule.class,
					PixSchedulingPatchShouldNotBeUsedOnAuthorisedConsent.class,
					PixSchedulingPatchHappyPathRevokedByUserTestModule.class,
					PixSchedulingPatchHappyPathRevokedByTppTestModule.class,
					PixSchedulingPatchConsentsIncorrectStatusTestModule.class,
					PixSchedulingPatchConsentsMissingLoggedUserTestModule.class,
					PixScheduledPaymentContentTypeJwtTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
