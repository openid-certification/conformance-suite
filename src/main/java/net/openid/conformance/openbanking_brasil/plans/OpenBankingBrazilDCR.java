package net.openid.conformance.openbanking_brasil.plans;


import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.PreFlightCertCheckPaymentsModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Banking Brazil - DCR",
	profile = OBBProfile.OBB_PROFIlE_PHASE3,
	displayName = PlanNames.OBB_DCR,
	summary = "summary"
)
public class OpenBankingBrazilDCR implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckPaymentsModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
