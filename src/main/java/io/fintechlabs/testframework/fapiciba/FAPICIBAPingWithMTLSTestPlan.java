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
		"fapi-ciba-ping-with-mtls-ensure-request-object-bad-aud-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-missing-iss-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-bad-iss-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-missing-exp-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-expired-exp-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-exp-is-year-in-future-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-missing-iat-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-iat-is-week-in-past-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-iat-is-hour-in-future-fails",
		"fapi-ciba-ping-with-mtls-ensure-request-object-missing-jti-fails",
	}
)
public class FAPICIBAPingWithMTLSTestPlan implements TestPlan {

}
