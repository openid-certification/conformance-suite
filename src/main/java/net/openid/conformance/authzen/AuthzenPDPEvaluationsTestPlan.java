package net.openid.conformance.authzen;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-evaluations-test-plan",
	displayName = "Authzen 1.0: PDP server test for batch evaluations - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Interop 1.1 cases
		AuthzenPDPEvaluations01Test.class,
		AuthzenPDPEvaluations02Test.class,
		AuthzenPDPEvaluations03Test.class
	}
)
public class AuthzenPDPEvaluationsTestPlan implements TestPlan {
}
