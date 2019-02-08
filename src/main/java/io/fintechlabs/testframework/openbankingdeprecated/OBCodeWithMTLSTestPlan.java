package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author ddrysdale
 *
 */
@PublishTestPlan (
	testPlanName = "ob-code-with-mtls-test-plan",
	displayName = "OB: code with mtls Test Plan",
	profile = "OB",
	testModuleNames = {
		"ob-discovery-end-point-verification",
		"ob-code-with-mtls",
		"ob-ensure-matls-required-code-with-mtls",
		"ob-ensure-matching-key-in-authorization-request-code-with-mtls",
		"ob-ensure-redirect-uri-in-authorization-request-code-with-mtls",
		"ob-ensure-registered-certificate-for-authorization-code-code-with-mtls",
		"ob-ensure-registered-redirect-uri-code-with-mtls",
		"ob-ensure-request-object-signature-algorithm-is-not-none-code-with-mtls",
		"ob-user-rejects-authentication-code-with-mtls"
	}
)
public class OBCodeWithMTLSTestPlan implements TestPlan {

}
