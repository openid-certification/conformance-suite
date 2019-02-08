package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-deprecated-eol-sept-2019-code-id-token-with-secret-basic-and-matls-test-plan",
	displayName = "OB-deprecated-EOL-sept-2019: code id_token with secret basic and matls Test Plan",
	profile = "OB-deprecated-EOL-Sept-2019",
	testModuleNames = {
		"ob-discovery-end-point-verification",
		"ob-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-matls-required-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-matching-key-in-authorization-request-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-redirect-uri-in-authorization-request-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-registered-certificate-for-authorization-code-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-registered-redirect-uri-code-id-token-with-secret-basic-and-matls",
		"ob-ensure-request-object-signature-algorithm-is-not-none-code-id-token-with-secret-basic-and-matls",
		"ob-user-rejects-authentication-code-id-token-with-secret-basic-and-matls",
	}
)
public class OBCodeIdTokenWithSecretBasicAndMATLSTestPlan implements TestPlan {

}
