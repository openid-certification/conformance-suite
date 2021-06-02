package net.openid.conformance.raidiam;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Raidiam Services",
	profile = RaidiamProfile.RAIDIAM_PROFILE,
	displayName = "Test simple access to a protected resource",
	summary = "Calls resources on the directory, largely to prove the FAPI security profile",
	testModules = {
		RaidiamOrgApiTest.class
	})
public class RaidiamDirectoryTestPlan implements TestPlan {
}
