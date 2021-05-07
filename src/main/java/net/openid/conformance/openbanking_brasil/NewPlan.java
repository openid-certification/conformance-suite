package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "simple-plan",
	profile = "A simple test", // A sub-heading in the test plan dropdown
	displayName = "A simple plan",// This appears in the test plan dropdown
	summary = "describe", // a short description
	testModules = {
		NewTest.class
	})
public class NewPlan implements TestPlan {
}
