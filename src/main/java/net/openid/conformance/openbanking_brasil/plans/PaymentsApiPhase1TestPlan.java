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
					PaymentsApiTestModule.class,
					PaymentsConsentsApiTestModule.class,
					PaymentsConsumedConsentsTestModule.class,
					PaymentsConsentsApiEnforceMANUTestModule.class,
					PaymentsConsentsApiMANUPixResponseTestModule.class,
					PaymentsConsentsApiEnforceDICTTestModule.class,
					PaymentsConsentsApiDICTPixResponseTestModule.class,
					PaymentsConsentsApiINICPixResponseTestModule.class,
					PaymentsConsentsApiEmailAddressProxyTestModule.class,
					PaymentsConsentsApiPhoneNumberProxyTestModule.class,
					PaymentsConsentsApiIncorrectCPFProxyTestModule.class,
					PaymentsConsentsApiBadPaymentTypeTestModule.class,
					PaymentsConsentsInvalidPersonTypeTestModule.class,
					PaymentsConsentsReuseJtiTestModule.class,
					PaymentsApiFapiTesting.class,
					PaymentsApiBadPaymentSignatureFails.class,
					PaymentsApiSignatureTest.class,
					PaymentsApiUnregisteredCnpjTestModule.class,
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
