package net.openid.conformance.openbanking_brasil.plans.channels;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.channels.ChannelsBankingAgentsApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Channels - BankingAgents API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.BANKING_AGENTS_API_TEST_PLAN,
	summary = "Structural and logical tests for Channels - BankingAgents API"
)
public class ChannelsBankingAgentsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					ChannelsBankingAgentsApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
