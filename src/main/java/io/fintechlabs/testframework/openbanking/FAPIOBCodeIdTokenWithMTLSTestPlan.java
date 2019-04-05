package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ob-code-id-token-with-mtls-test-plan",
	displayName = "FAPI-OB: code id_token with mtls Test Plan",
	profile = "FAPI-OB",
	testModuleNames = {
		"fapi-ob-discovery-end-point-verification",
		"fapi-ob-code-id-token-with-mtls",
		"fapi-ob-ensure-matls-required-code-id-token-with-mtls",
		"fapi-ob-ensure-matching-key-in-authorization-request-code-id-token-with-mtls",
		"fapi-ob-ensure-redirect-uri-in-authorization-request-code-id-token-with-mtls",
		"fapi-ob-ensure-registered-certificate-for-authorization-code-code-id-token-with-mtls",
		"fapi-ob-ensure-registered-redirect-uri-code-id-token-with-mtls",
		"fapi-ob-ensure-request-object-signature-algorithm-is-not-none-code-id-token-with-mtls",
		"fapi-ob-user-rejects-authentication-code-id-token-with-mtls",
		"fapi-ob-ensure-server-handles-non-matching-intent-id-code-id-token-with-mtls",
		"fapi-ob-ensure-request-object-without-exp-fails-with-mtls",
		"fapi-ob-ensure-request-object-without-scope-fails-with-mtls",
		"fapi-ob-ensure-request-object-without-state-with-mtls",
		"fapi-ob-ensure-request-object-without-nonce-fails-with-mtls",
		"fapi-ob-ensure-request-object-without-redirect-uri-fails-with-mtls",
		"fapi-ob-ensure-request-object-with-multiple-aud-succeeds-with-mtls",
		"fapi-ob-ensure-wrong-client-id-in-token-endpoint-fails-with-mtls",
		"fapi-ob-ensure-expired-request-object-fails-with-mtls",
		"fapi-ob-ensure-different-nonce-inside-and-outside-request-object-with-mtls",
		"fapi-ob-ensure-response-type-code-fails-with-mtls",
		"fapi-ob-ensure-request-object-with-bad-aud-fails-with-mtls",
		"fapi-ob-ensure-signed-request-object-with-RS256-fails-with-mtls",
	}
)
public class FAPIOBCodeIdTokenWithMTLSTestPlan implements TestPlan {

}
