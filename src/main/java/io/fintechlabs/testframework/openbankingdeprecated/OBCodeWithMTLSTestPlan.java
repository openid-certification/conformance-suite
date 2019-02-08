package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-deprecated-eol-sept-2019-code-with-mtls-test-plan",
	displayName = "OB-deprecated-EOL-sept-2019: code with mtls Test Plan",
	profile = "OB-deprecated-EOL-Sept-2019",
	testModuleNames = {
		"ob-deprecated-eol-sept-2019-discovery-end-point-verification",
		"ob-deprecated-eol-sept-2019-code-with-mtls",
		"ob-deprecated-eol-sept-2019-ensure-matls-required-code-with-mtls",
		"ob-deprecated-eol-sept-2019-ensure-matching-key-in-authorization-request-code-with-mtls",
		"ob-deprecated-eol-sept-2019-ensure-redirect-uri-in-authorization-request-code-with-mtls",
		"ob-deprecated-eol-sept-2019-ensure-registered-certificate-for-authorization-code-code-with-mtls",
		"ob-deprecated-eol-sept-2019-ensure-registered-redirect-uri-code-with-mtls",
		"ob-deprecated-eol-sept-2019-ensure-request-object-signature-algorithm-is-not-none-code-with-mtls",
		"ob-deprecated-eol-sept-2019-user-rejects-authentication-code-with-mtls"
	}
)
public class OBCodeWithMTLSTestPlan implements TestPlan {

}
