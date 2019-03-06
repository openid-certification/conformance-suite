package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-ping-with-mtls-test-plan",
	displayName = "FAPI-CIBA: ping with mtls client authentication test plan",
	profile = "FAPI-CIBA",
	testModuleNames = {
		"fapi-ciba-ping-with-mtls",
	}
)
public class FAPICIBAPingWithMTLSTestPlan implements TestPlan {

}
