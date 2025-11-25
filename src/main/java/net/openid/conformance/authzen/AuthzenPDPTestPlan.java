package net.openid.conformance.authzen;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-test-plan",
	displayName = "Authzen 1.0: PDP server test - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	testModules = {
		// Interop 1.1 cases
		AuthzenPDPEvaluation01Test.class,
		AuthzenPDPEvaluation02Test.class,
		AuthzenPDPEvaluation03Test.class,
		AuthzenPDPEvaluation04Test.class,
		AuthzenPDPEvaluation05Test.class,
		AuthzenPDPEvaluation06Test.class,
		AuthzenPDPEvaluation07Test.class,
		AuthzenPDPEvaluation08Test.class,
		AuthzenPDPEvaluation09Test.class,
		AuthzenPDPEvaluation10Test.class,
		AuthzenPDPEvaluation11Test.class,
		AuthzenPDPEvaluation12Test.class,
		AuthzenPDPEvaluation13Test.class,
		AuthzenPDPEvaluation14Test.class,
		AuthzenPDPEvaluation15Test.class,
		AuthzenPDPEvaluation16Test.class,
		AuthzenPDPEvaluation17Test.class,
		AuthzenPDPEvaluation18Test.class,
		AuthzenPDPEvaluation19Test.class,
		AuthzenPDPEvaluation20Test.class,
		AuthzenPDPEvaluation21Test.class,
		AuthzenPDPEvaluation22Test.class,
		AuthzenPDPEvaluation23Test.class,
		AuthzenPDPEvaluation24Test.class,
		AuthzenPDPEvaluation25Test.class,
		AuthzenPDPEvaluation26Test.class,
		AuthzenPDPEvaluation27Test.class,
		AuthzenPDPEvaluation28Test.class,
		AuthzenPDPEvaluation29Test.class,
		AuthzenPDPEvaluation30Test.class,
		AuthzenPDPEvaluation31Test.class,
		AuthzenPDPEvaluation32Test.class,
		AuthzenPDPEvaluation33Test.class,
		AuthzenPDPEvaluation34Test.class,
		AuthzenPDPEvaluation35Test.class,
		AuthzenPDPEvaluation36Test.class,
		AuthzenPDPEvaluation37Test.class,
		AuthzenPDPEvaluation38Test.class,
		AuthzenPDPEvaluation39Test.class,
		AuthzenPDPEvaluation40Test.class
	}
)
public class AuthzenPDPTestPlan implements TestPlan {
}
