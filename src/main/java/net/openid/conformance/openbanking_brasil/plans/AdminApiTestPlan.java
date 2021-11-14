package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AdminApiTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Admin api test",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	displayName = PlanNames.ADMIN_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Admin API"
)
public class AdminApiTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					AdminApiTestModule.class

				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}
}
