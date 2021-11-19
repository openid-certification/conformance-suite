package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.channels.ChannelsApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Channels api test",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.CHANNELS_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Channels API"
)
public class ChannelsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					ChannelsApiTestModule.class

				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
