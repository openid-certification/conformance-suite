package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2-OpenBankingUK: Authorization server test using private_key_jwt client authentication",
	profile = "FAPI-RW-ID2-OpenBankingUK-OpenID-Provider-Authorization-Server-Test",
	testModuleNames = {
		"fapi-rw-id2-ob-discovery-end-point-verification",
		"fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-mtls-holder-of-key-required-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-matching-key-in-authorization-request-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-redirect-uri-in-authorization-request-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-authorization-code-is-bound-to-client-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-registered-redirect-uri-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-signature-algorithm-is-not-none-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-user-rejects-authentication-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-server-handles-non-matching-intent-id-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-without-exp-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-without-scope-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-without-state-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-without-nonce-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-without-redirect-uri-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-with-multiple-aud-succeeds-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-wrong-client-id-in-token-endpoint-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-expired-request-object-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-different-nonce-inside-and-outside-request-object-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-response-type-code-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-request-object-with-bad-aud-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-signed-client-assertion-with-RS256-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-signed-request-object-with-RS256-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-authorization-request-without-request-object-fails-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-client-assertion-in-token-endpoint-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-response-mode-query-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-authorization-request-without-state-success-with-private-key-and-mtls-holder-of-key",
	}
)
public class FAPIRWID2OBWithPrivateKeyAndMTLSHolderOfKeyTestPlan implements TestPlan {

}
