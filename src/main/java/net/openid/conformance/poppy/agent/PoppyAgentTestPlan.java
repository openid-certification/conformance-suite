package net.openid.conformance.poppy.agent;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "poppy-agent-test-plan",
	shortName = "Agent happy flow",
	certifiable = false,
	alpha = true,
	displayName = "Poppy 0.4 Personal Agent test plan (alpha, not part of a certification program)",
	profile = TestPlan.ProfileNames.poppyAgentTest,
	specFamily = TestPlan.SpecFamilyNames.poppy,
	testModules = {
		PoppyAgentHappyFlow.class
	}
)
public class PoppyAgentTestPlan implements TestPlan {
}
