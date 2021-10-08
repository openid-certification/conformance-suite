package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Payments api phase 2 test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.PAYMENTS_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant payments API including QR tests"
)
public class PaymentsApiPhase2TestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PaymentsConsentsApiEnforceQRDNTestModule.class,
					PaymentsConsentsApiEnforceQRESTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
