package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-deprecated-eol-sept-2019-code-id-token-with-mtls-test-plan",
	displayName = "OB-deprecated-EOL-sept-2019: code id_token with mtls Test Plan",
	profile = "OB-deprecated-EOL-Sept-2019",
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
	}
)
public class OBCodeIdTokenWithMTLSTestPlan implements TestPlan {

}
