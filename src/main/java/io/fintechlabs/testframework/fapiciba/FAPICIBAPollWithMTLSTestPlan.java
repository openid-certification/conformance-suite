package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-poll-with-mtls-test-plan",
	displayName = "FAPI-CIBA: poll with mtls client authentication test plan",
	profile = "FAPI-CIBA",
	testModuleNames = {
		"fapi-ciba-poll-discovery-end-point-verification",
		"fapi-ciba-poll-with-mtls",
		"fapi-ciba-poll-with-mtls-ensure-request-object-missing-aud-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-bad-aud-fails",
	}
)
public class FAPICIBAPollWithMTLSTestPlan implements TestPlan {

}
