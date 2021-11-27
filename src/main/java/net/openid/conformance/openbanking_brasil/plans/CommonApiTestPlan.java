package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.CommonApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;
@PublishTestPlan(
	testPlanName = "Common api test",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.COMMON_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Common API"
)
public class CommonApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CommonApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
