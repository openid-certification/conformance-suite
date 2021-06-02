package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.raidiam.RaidiamProfile;

/**
 * CunningPlan
 *
 * This is a very simple test plan which has two modules in it. The
 * purpose is to see how those modules contribute to the configuration
 * UI, mostly.
 */
@PublishTestPlan(
	testPlanName = "cunning-plan",
	profile = RaidiamProfile.RAIDIAM_PROFILE,
	displayName = "A cunning plan",// This appears in the test plan dropdown
	summary = "More cunning than a fox who just got made professor of cunning at Oxford", // a short description
	testModules = {
		MoreConfigurableApiCallTest.class // these classes implement specific sets of tests
	})
public class CunningPlan implements TestPlan {
}
