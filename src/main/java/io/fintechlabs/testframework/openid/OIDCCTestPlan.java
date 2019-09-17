package io.fintechlabs.testframework.openid;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-test-plan",
	displayName = "OIDCC: Authorization server test",
	profile = "OIDCC",
	testModules = {
		OIDCC.class
	}
)
public class OIDCCTestPlan implements TestPlan {

}
