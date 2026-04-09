package net.openid.conformance.authzen.interop.evaluations;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-interop-evaluations-test-plan",
	displayName = "Authzen 1.0: PDP Interop server test for batch evaluations - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Interop 1.1 cases
		AuthzenPDPInteropEvaluations01Test.class,
		AuthzenPDPInteropEvaluations02Test.class,
		AuthzenPDPInteropEvaluations03Test.class
	}
)
public class AuthzenPDPInteropEvaluationsTestPlan implements TestPlan {
}
