package io.fintechlabs.testframework.openid;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-test-plan",
	displayName = "OIDCC: Authorization server test",
	profile = "OIDCC",
	testModules = {
		OIDCC.class,
		OIDCCRefreshToken.class
	},
	variants = {
		OIDCC.variant_client_secret_post,
		OIDCC.variant_client_secret_jwt,
		OIDCC.variant_private_key_jwt,
		OIDCC.variant_mtls
	}
)
public class OIDCCTestPlan implements TestPlan {

}
