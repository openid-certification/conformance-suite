package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-with-mtls-test-plan",
	displayName = "FAPI-RW-ID2-OpenBankingUK: Authorization server test using mtls client authentication",
	profile = "FAPI-RW-ID2-OpenBankingUK-OpenID-Provider-Authorization-Server-Test",
	testModuleNames = {
		// Normal well behaved client cases
		"fapi-rw-id2-ob-discovery-end-point-verification",
		"fapi-rw-id2-ob-with-mtls",
		"fapi-rw-id2-ob-user-rejects-authentication-with-mtls",
		"fapi-rw-id2-ob-ensure-request-object-with-multiple-aud-succeeds-with-mtls",
		"fapi-rw-id2-ob-ensure-authorization-request-without-state-success-with-mtls",

		// Possible failure case
		"fapi-rw-id2-ob-ensure-response-mode-query-with-mtls",
		"fapi-rw-id2-ob-ensure-different-nonce-inside-and-outside-request-object-with-mtls",
		"fapi-rw-id2-ob-ensure-registered-redirect-uri-with-mtls",

		// Negative tests for request objects
		"fapi-rw-id2-ob-ensure-request-object-without-exp-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-request-object-without-scope-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-request-object-without-state-with-mtls",
		"fapi-rw-id2-ob-ensure-request-object-without-nonce-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-request-object-without-redirect-uri-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-expired-request-object-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-request-object-with-bad-aud-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-signed-request-object-with-RS256-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-request-object-signature-algorithm-is-not-none-with-mtls",
		"fapi-rw-id2-ob-ensure-matching-key-in-authorization-request-with-mtls",

		// Negative tests for authorization request
		"fapi-rw-id2-ob-ensure-authorization-request-without-request-object-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-redirect-uri-in-authorization-request-with-mtls",
		"fapi-rw-id2-ob-ensure-response-type-code-fails-with-mtls",

		// Negative tests for token endpoint
		"fapi-rw-id2-ob-ensure-wrong-client-id-in-token-endpoint-fails-with-mtls",
		"fapi-rw-id2-ob-ensure-mtls-holder-of-key-required-with-mtls",
		"fapi-rw-id2-ob-ensure-authorization-code-is-bound-to-client-with-mtls",

		// OB systems specific tests
		"fapi-rw-id2-ob-ensure-server-handles-non-matching-intent-id-with-mtls",
	}
)
public class FAPIRWID2OBWithMTLSTestPlan implements TestPlan {

}
