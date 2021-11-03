package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.fapi1advancedfinal.PaymentsApiDcrTestModuleAttemptClientTakeover;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentsApiBadPaymentSignatureFails;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentsApiFapiTesting;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentApiNoDebtorProvidedTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiDcrHappyFlowTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiDcrTestModuleUnauthorizedClient;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiIncorrectCPFProxyTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiInvalidCnpjTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiNegativeTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiRealEmailAddressWrongCreditorProxyTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsApiWrongEmailAddressProxyTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiBadPaymentTypeTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiDICTPixResponseTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiDateTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiEmailAddressProxyTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiEnforceDICTTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiEnforceMANUTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiForceCheckBadSignatureTest;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiINICPixResponseTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiMANUPixResponseTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiPhoneNumberProxyTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsInvalidPersonTypeTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsJsonAcceptHeaderJwtReturnedTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsReuseIdempotencyKeyTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PaymentsConsentsReuseJtiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.PaymentsConsentsApiInvalidTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.PaymentsConsumedConsentsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Payments api phase 1 test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.PAYMENTS_API_PHASE_1_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant payments API"
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
