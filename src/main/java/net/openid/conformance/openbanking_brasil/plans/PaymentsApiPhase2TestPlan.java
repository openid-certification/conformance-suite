package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckModule;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Payments Api Phase 2 - QRCodes",
	profile = OBBProfile.OBB_PROFIlE_PHASE3,
	displayName = PlanNames.PAYMENTS_API_PHASE_2_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant payments API including QR tests. These tests are designed to validate primarily the QRES consents structure ensuring structural integrity and content validation. QRDN tests are pending an agreed approach with Squad Sandbox due to the requirement to integrate with Banco Central for their generation."
)
public class PaymentsApiPhase2TestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					PaymentsConsentsApiEnforceQRDNTestModule.class,
					PaymentsConsentsApiEnforceQRESTestModule.class,
					PaymentsConsentsApiEnforceQRESNWithEmailAddressTestModule.class,
					PaymentsConsentsApiEnforceQRESWithPhoneNumberTestModule.class,
					PaymentsConsentsApiEnforceQRESWrongAmountTestModule.class,
					PaymentsConsentsApiEnforceQRESWrongProxyTestModule.class,
					PaymentsConsentsApiEnforceQRDNWithQRESCodeTestModule.class,
					PaymentsConsentsApiQRDNHappyTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
