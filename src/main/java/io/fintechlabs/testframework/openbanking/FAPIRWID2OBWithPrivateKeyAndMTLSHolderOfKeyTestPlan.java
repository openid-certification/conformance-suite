package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jheenan
 *
 */
@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2-OB: with private key and mtls holder of key Test Plan",
	profile = "FAPI-RW-ID2-OB",
	testModuleNames = {
		"fapi-rw-id2-ob-discovery-end-point-verification",
		"fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-mtls-holder-of-key-required-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-matching-key-in-authorization-request-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-redirect-uri-in-authorization-request-with-private-key-and-mtls-holder-of-key",
		"fapi-rw-id2-ob-ensure-registered-certificate-for-authorization-code-with-private-key-and-mtls-holder-of-key",
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
	}
)
public class FAPIRWID2OBWithPrivateKeyAndMTLSHolderOfKeyTestPlan implements TestPlan {

}
