package net.openid.conformance.openbanking_brasil.plans.channels;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.channels.ChannelsPhoneChannelsApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Channels - Phone Channels API test plan",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.PHONE_CHANNELS_API_TEST_PLAN,
	summary = "Structural and logical tests for Channels - Phone Channels API"
)
public class ChannelsPhoneChannelsApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					ChannelsPhoneChannelsApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
