package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-code-with-secret-post-and-matls-test-plan",
	displayName = "OB: code with secret post and matls Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-discovery-end-point-verification",
		"ob-code-with-secret-post-and-matls",
		"ob-ensure-matls-required-code-with-secret-post-and-matls",
		"ob-ensure-matching-key-in-authorization-request-code-with-secret-post-and-matls",
		"ob-ensure-redirect-uri-in-authorization-request-code-with-secret-post-and-matls",
		"ob-ensure-registered-certificate-for-authorization-code-code-with-secret-post-and-matls",
		"ob-ensure-registered-redirect-uri-code-with-secret-post-and-matls",
		"ob-ensure-request-object-signature-algorithm-is-not-none-code-with-secret-post-and-matls",
		"ob-user-rejects-authentication-code-with-secret-post-and-matls"
	}
)
public class OBCodeWithSecretPostAndMATLSTestPlan implements TestPlan {

}
