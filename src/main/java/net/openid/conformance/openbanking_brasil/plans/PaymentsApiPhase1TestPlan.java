package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentsApiBadPaymentSignatureFails;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentsApiFapiTesting;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.PaymentsConsentsApiInvalidTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.PaymentsConsumedConsentsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Payments api phase 1 test",
	profile = OBBProfile.OBB_PROFIlE_PHASE3,
	displayName = PlanNames.PAYMENTS_API_PHASE_1_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil payments API for INIC, MANU and DICT - Submissions for this test plan should only be done until 14/01/22. After this date, the service desk will only accept submissions that also include QRES and QRDN modules"
)
public class PaymentsApiPhase1TestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					PaymentsApiTestModule.class,
					PaymentsApiNegativeTestModule.class,
					PaymentApiNoDebtorProvidedTestModule.class,
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
					PaymentsApiFapiTesting.class,
					PaymentsApiDcrHappyFlowTestModule.class,
					PaymentsApiDcrTestModuleUnauthorizedClient.class,
					PaymentsApiDcrTestModuleAttemptClientTakeover.class,
					PaymentsApiDcrSubjectDn.class,
					PaymentsApiBadPaymentSignatureFails.class,
					PaymentsConsentsApiForceCheckBadSignatureTest.class,
					PaymentsApiInvalidCnpjTestModule.class,
					PaymentsConsentsApiInvalidTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
