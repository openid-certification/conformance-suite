package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-code-with-private-key-and-matls-test-plan",
	displayName = "OB: code with private key and matls Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-discovery-end-point-verification",
		"ob-code-with-private-key-and-matls",
		"ob-ensure-matls-required-code-with-private-key-and-matls",
		"ob-ensure-matching-key-in-authorization-request-code-with-private-key-and-matls",
		"ob-ensure-redirect-uri-in-authorization-request-code-with-private-key-and-matls",
		"ob-ensure-registered-certificate-for-authorization-code-code-with-private-key-and-matls",
		"ob-ensure-registered-redirect-uri-code-with-private-key-and-matls",
		"ob-ensure-request-object-signature-algorithm-is-not-none-code-with-private-key-and-matls",
		"ob-user-rejects-authentication-code-with-private-key-and-matls"
	}
)
public class OBCodeWithPrivateKeyAndMATLSTestPlan implements TestPlan {

}
