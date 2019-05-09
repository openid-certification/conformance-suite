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
		"fapi-ciba-poll-user-rejects-authentication-with-mtls",
		"fapi-ciba-poll-with-mtls-ensure-request-object-missing-aud-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-bad-aud-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-missing-iss-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-bad-iss-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-missing-exp-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-expired-exp-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-exp-is-year-in-future-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-missing-iat-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-iat-is-week-in-past-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-iat-is-hour-in-future-fails",
		"fapi-ciba-poll-with-mtls-ensure-request-object-missing-jti-fails",
		"fapi-ciba-poll-ensure-authorization-request-with-multiple-hints-fails-with-mtls",
	}
)
public class FAPICIBAPollWithMTLSTestPlan implements TestPlan {

}
