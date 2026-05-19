package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-interop-evaluation-test-plan",
	displayName = "Authzen 1.0: PDP Interop server test for evaluation - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Interop 1.1 cases
		AuthzenPDPInteropEvaluation01Test.class,
		AuthzenPDPInteropEvaluation02Test.class,
		AuthzenPDPInteropEvaluation03Test.class,
		AuthzenPDPInteropEvaluation04Test.class,
		AuthzenPDPInteropEvaluation05Test.class,
		AuthzenPDPInteropEvaluation06Test.class,
		AuthzenPDPInteropEvaluation07Test.class,
		AuthzenPDPInteropEvaluation08Test.class,
		AuthzenPDPInteropEvaluation09Test.class,
		AuthzenPDPInteropEvaluation10Test.class,
		AuthzenPDPInteropEvaluation11Test.class,
		AuthzenPDPInteropEvaluation12Test.class,
		AuthzenPDPInteropEvaluation13Test.class,
		AuthzenPDPInteropEvaluation14Test.class,
		AuthzenPDPInteropEvaluation15Test.class,
		AuthzenPDPInteropEvaluation16Test.class,
		AuthzenPDPInteropEvaluation17Test.class,
		AuthzenPDPInteropEvaluation18Test.class,
		AuthzenPDPInteropEvaluation19Test.class,
		AuthzenPDPInteropEvaluation20Test.class,
		AuthzenPDPInteropEvaluation21Test.class,
		AuthzenPDPInteropEvaluation22Test.class,
		AuthzenPDPInteropEvaluation23Test.class,
		AuthzenPDPInteropEvaluation24Test.class,
		AuthzenPDPInteropEvaluation25Test.class,
		AuthzenPDPInteropEvaluation26Test.class,
		AuthzenPDPInteropEvaluation27Test.class,
		AuthzenPDPInteropEvaluation28Test.class,
		AuthzenPDPInteropEvaluation29Test.class,
		AuthzenPDPInteropEvaluation30Test.class,
		AuthzenPDPInteropEvaluation31Test.class,
		AuthzenPDPInteropEvaluation32Test.class,
		AuthzenPDPInteropEvaluation33Test.class,
		AuthzenPDPInteropEvaluation34Test.class,
		AuthzenPDPInteropEvaluation35Test.class,
		AuthzenPDPInteropEvaluation36Test.class,
		AuthzenPDPInteropEvaluation37Test.class,
		AuthzenPDPInteropEvaluation38Test.class,
		AuthzenPDPInteropEvaluation39Test.class,
		AuthzenPDPInteropEvaluation40Test.class
	}
)
public class AuthzenPDPInteropEvaluationTestPlan implements TestPlan {
}
