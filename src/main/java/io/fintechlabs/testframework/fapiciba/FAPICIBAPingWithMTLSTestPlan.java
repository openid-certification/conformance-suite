package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-ping-with-mtls-test-plan",
	displayName = "FAPI-CIBA: ping with mtls client authentication test plan",
	profile = "FAPI-CIBA",
	testModuleNames = {
		"fapi-ciba-ping-discovery-end-point-verification",
		"fapi-ciba-ping-with-mtls",
		"fapi-ciba-ping-with-mtls-ensure-request-object-missing-aud-fails",
	}
)
public class FAPICIBAPingWithMTLSTestPlan implements TestPlan {

}
