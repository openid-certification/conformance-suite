package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-code-id-token-with-mtls-test-plan",
	displayName = "OB: code id_token with mtls Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-discovery-end-point-verification",
		"ob-code-id-token-with-mtls",
		"ob-ensure-matls-required-code-id-token-with-mtls",
		"ob-ensure-matching-key-in-authorization-request-code-id-token-with-mtls",
		"ob-ensure-redirect-uri-in-authorization-request-code-id-token-with-mtls",
		"ob-ensure-registered-certificate-for-authorization-code-code-id-token-with-mtls",
		"ob-ensure-registered-redirect-uri-code-id-token-with-mtls",
		"ob-ensure-request-object-signature-algorithm-is-not-none-code-id-token-with-mtls",
		"ob-user-rejects-authentication-code-id-token-with-mtls",
		"ob-ensure-server-handles-non-matching-intent-id-code-id-token-with-mtls",
		"ob-ensure-request-object-without-exp-fails-with-mtls",
		"ob-ensure-request-object-without-scope-fails-with-mtls",
		"ob-ensure-request-object-without-state-fails-with-mtls",
		"ob-ensure-request-object-without-nonce-fails-with-mtls",
	}
)
public class OBCodeIdTokenWithMTLSTestPlan implements TestPlan {

}
