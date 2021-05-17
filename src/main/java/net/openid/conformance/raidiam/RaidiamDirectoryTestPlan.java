package net.openid.conformance.raidiam;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Raidiam Services",
	profile = "Raidiam Directory Tests",
	displayName = "Test simple access to a resource",
	summary = "Calls resources on the directory, largely to prove the FAPI security profile",
	testModules = {
		RaidiamOrgApiTest.class
	})
public class RaidiamDirectoryTestPlan implements TestPlan {
}
