package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-with-mtls-test-plan",
	displayName = "FAPI-RW-ID2: Authorization server test using mtls client authentication",
	profile = "FAPI-RW-ID2-OpenID-Provider-Authorization-Server-Test",
	testModuleNames = {
		"fapi-rw-id2-discovery-end-point-verification",
		"fapi-rw-id2-with-mtls",
		"fapi-rw-id2-ensure-request-object-signature-algorithm-is-not-null",
		"fapi-rw-id2-ensure-client-id-in-token-endpoint-with-mtls",
		"fapi-rw-id2-ensure-request-object-without-exp-fails-with-mtls",
		"fapi-rw-id2-ensure-request-object-without-scope-fails-with-mtls",
		"fapi-rw-id2-ensure-request-object-without-state-with-mtls",
		"fapi-rw-id2-ensure-request-object-without-nonce-fails-with-mtls",
		"fapi-rw-id2-ensure-request-object-without-redirect-uri-fails-with-mtls",
		"fapi-rw-id2-ensure-request-object-with-multiple-aud-succeeds-with-mtls",
		"fapi-rw-id2-ensure-expired-request-object-fails-with-mtls",
		"fapi-rw-id2-ensure-different-nonce-inside-and-outside-request-object-with-mtls",
		"fapi-rw-id2-ensure-response-type-code-fails-with-mtls",
		"fapi-rw-id2-ensure-request-object-with-bad-aud-fails-with-mtls",
		"fapi-rw-id2-ensure-mtls-holder-of-key-required-with-mtls",
		"fapi-rw-id2-ensure-matching-key-in-authorization-request-with-mtls",
		"fapi-rw-id2-ensure-redirect-uri-in-authorization-request-with-mtls",
		"fapi-rw-id2-ensure-authorization-code-is-bound-to-client-with-mtls",
		"fapi-rw-id2-ensure-registered-redirect-uri-with-mtls",
		"fapi-rw-id2-ensure-request-object-signature-algorithm-is-not-none-with-mtls",
		"fapi-rw-id2-user-rejects-authentication-with-mtls",
		"fapi-rw-id2-ensure-signed-request-object-with-RS256-fails-with-mtls",
		"fapi-rw-id2-ensure-authorization-request-without-request-object-fails-with-mtls",
		"fapi-rw-id2-ensure-response-mode-query-with-mtls",
		"fapi-rw-id2-ensure-authorization-request-without-state-success-with-mtls"
	}
)
public class FAPI_RW_ID2_WithMTLSTestPlan implements TestPlan {

}
