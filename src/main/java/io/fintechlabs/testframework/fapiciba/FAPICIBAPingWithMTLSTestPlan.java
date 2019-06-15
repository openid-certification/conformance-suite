package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-ping-with-mtls-test-plan",
	displayName = "FAPI-CIBA: ping with mtls client authentication test plan",
	profile = "FAPI-CIBA",
	testModuleNames = {
		"fapi-ciba-ping-discovery-end-point-verification",
		"fapi-ciba",
		"fapi-ciba-ping-user-rejects-authentication-with-mtls",
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
		"fapi-ciba-ping-ensure-authorization-request-with-multiple-hints-fails-with-mtls",
		"fapi-ciba-ping-ensure-request-object-signature-algorithm-is-none-fails-with-mtls",
		"fapi-ciba-ping-ensure-request-object-signature-algorithm-is-bad-fails-with-mtls",
		"fapi-ciba-ping-ensure-request-object-signature-algorithm-is-RS256-fails-with-mtls",
		"fapi-ciba-ping-ensure-request-object-signed-by-other-client-fails-with-mtls",
		"fapi-ciba-ping-ensure-authorization-request-with-binding-message-succeeds-with-mtls",
		"fapi-ciba-ping-ensure-authorization-request-with-potentially-bad-binding-message-with-mtls",
		"fapi-ciba-auth-req-id-expired",
		"fapi-ciba-ping-ensure-backchannel-authorization-request-without-request-fails-with-mtls",
		"fapi-ciba-ping-multiple-call-to-token-endpoint-with-mtls",
		"fapi-ciba-ping-ensure-different-client-id-and-issuer-in-backchannel-authorization-request-with-mtls",
		"fapi-ciba-ping-ensure-wrong-client-id-in-token-endpoint-request-with-mtls",
		"fapi-ciba-ping-ensure-wrong-client-id-in-backchannel-authorization-request-with-mtls",
		"fapi-ciba-ping-ensure-wrong-auth-req-id-in-token-endpoint-request-with-mtls",
		"fapi-ciba-ping-with-mtls-backchannel-notification-endpoint-response-has-body",
		"fapi-ciba-ping-with-mtls-backchannel-notification-endpoint-response-401",
		"fapi-ciba-ping-with-mtls-backchannel-notification-endpoint-response-403",
	}
)
public class FAPICIBAPingWithMTLSTestPlan implements TestPlan {

}
