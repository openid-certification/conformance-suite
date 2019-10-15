package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-test-plan",
	displayName = "OIDCC: Authorization server test",
	profile = "OIDCC",
	testModules = {
		OIDCC.class,
		OIDCCRefreshToken.class
	}
)
public class OIDCCTestPlan implements TestPlan {

}
