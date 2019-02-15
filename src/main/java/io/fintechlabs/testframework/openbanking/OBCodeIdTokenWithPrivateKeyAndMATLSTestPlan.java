package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jheenan
 *
 */
@PublishTestPlan (
	testPlanName = "ob-code-id-token-with-private-key-and-matls-test-plan",
	displayName = "OB: code id_token with private key and matls Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-discovery-end-point-verification",
		"ob-code-id-token-with-private-key-and-matls",
		"ob-ensure-matls-required-code-id-token-with-private-key-and-matls",
		"ob-ensure-matching-key-in-authorization-request-code-id-token-with-private-key-and-matls",
		"ob-ensure-redirect-uri-in-authorization-request-code-id-token-with-private-key-and-matls",
		"ob-ensure-registered-certificate-for-authorization-code-code-id-token-with-private-key-and-matls",
		"ob-ensure-registered-redirect-uri-code-id-token-with-private-key-and-matls",
		"ob-ensure-request-object-signature-algorithm-is-not-none-code-id-token-with-private-key-and-matls",
		"ob-user-rejects-authentication-code-id-token-with-private-key-and-matls",
		"ob-ensure-server-handles-non-matching-intent-id-code-id-token-with-private-key-and-matls",
		"ob-ensure-request-object-without-exp-fails-with-private-key-and-matls",
		"ob-ensure-request-object-without-scope-fails-with-private-key-and-matls",
		"ob-ensure-request-object-without-state-fails-with-private-key-and-matls",
		"ob-ensure-request-object-without-nonce-fails-with-private-key-and-matls",
		"ob-ensure-request-object-without-redirect-uri-fails-with-private-key-and-matls",
		"ob-ensure-request-object-with-multiple-aud-succeeds-with-private-key-and-matls",
		"ob-ensure-wrong-client-id-in-token-endpoint-fails-with-private-key-and-matls",
	}
)
public class OBCodeIdTokenWithPrivateKeyAndMATLSTestPlan implements TestPlan {

}
