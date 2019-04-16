package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "fapi-rw-id2-with-private-key-and-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2: with private key and mtls holder of key Test Plan",
	profile = "FAPI-RW-ID2",
	testModuleNames = {
		"fapi-rw-id2-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-discovery-end-point-verification",
		"fapi-rw-id2-ensure-client-id-in-token-endpoint-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-without-exp-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-without-scope-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-without-state-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-without-nonce-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-without-redirect-uri-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-with-multiple-aud-succeeds-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-expired-request-object-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-different-nonce-inside-and-outside-request-object-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-response-type-code-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-with-bad-aud-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-mtls-holder-of-key-required-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-matching-key-in-authorization-request-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-redirect-uri-in-authorization-request-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-authorization-code-is-bound-to-client-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-registered-redirect-uri-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-request-object-signature-algorithm-is-not-none-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-user-rejects-authentication-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-signed-client-assertion-with-RS256-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-signed-request-object-with-RS256-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-authorization-request-without-request-object-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-client-assertion-in-token-endpoint-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ensure-response-mode-query-with-private-key-and-mtls-holder-of-key",
	}
)
public class FAPI_RW_ID2_WithPrivateKeyAndMTLSHolderOfKeyTestPlan implements TestPlan {

}
