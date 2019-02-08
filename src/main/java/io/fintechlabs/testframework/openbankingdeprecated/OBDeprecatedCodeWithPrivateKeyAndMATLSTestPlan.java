package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-deprecated-eol-sept-2019-code-with-private-key-and-matls-test-plan",
	displayName = "OB-deprecated-EOL-sept-2019: code with private key and matls Test Plan",
	profile = "OB-deprecated-EOL-Sept-2019",
	testModuleNames = {
		"ob-deprecated-eol-sept-2019-discovery-end-point-verification",
		"ob-deprecated-eol-sept-2019-code-with-private-key-and-matls",
		"ob-deprecated-eol-sept-2019-ensure-matls-required-code-with-private-key-and-matls",
		"ob-deprecated-eol-sept-2019-ensure-matching-key-in-authorization-request-code-with-private-key-and-matls",
		"ob-deprecated-eol-sept-2019-ensure-redirect-uri-in-authorization-request-code-with-private-key-and-matls",
		"ob-deprecated-eol-sept-2019-ensure-registered-certificate-for-authorization-code-code-with-private-key-and-matls",
		"ob-deprecated-eol-sept-2019-ensure-registered-redirect-uri-code-with-private-key-and-matls",
		"ob-deprecated-eol-sept-2019-ensure-request-object-signature-algorithm-is-not-none-code-with-private-key-and-matls",
		"ob-deprecated-eol-sept-2019-user-rejects-authentication-code-with-private-key-and-matls"
	}
)
public class OBDeprecatedCodeWithPrivateKeyAndMATLSTestPlan implements TestPlan {

}
