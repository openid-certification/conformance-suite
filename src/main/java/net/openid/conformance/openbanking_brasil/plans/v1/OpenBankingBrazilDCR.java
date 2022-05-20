package net.openid.conformance.openbanking_brasil.plans.v1;


import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.DCRWithAllScopesNoUnregistrationTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Open Banking Brazil - DCR",
	profile = OBBProfile.OBB_PROFIlE_PHASE3,
	displayName = PlanNames.OBB_DCR,
	summary = "Tests for client registration via Dynamic Client Registration without unregistering the client after the test. " +
		"These tests are designed to help users execute the Conformance Suite locally against different Authorization Servers. " +
		"So, they can do a DCR using the C.S. and then do functional tests with the client_id created."
)
public class OpenBankingBrazilDCR implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					DCRWithAllScopesNoUnregistrationTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
