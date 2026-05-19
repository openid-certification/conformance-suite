package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "authzen-pdp-interop-resource-search-test-plan",
	displayName = "Authzen 1.0: PDP Interop server test for Resource Search - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.authzenTest,
	specFamily = TestPlan.SpecFamilyNames.authzen,
	testModules = {
		// Search Demo test cases
		// https://github.com/openid/authzen/blob/main/interop/authzen-search-demo/test/resource/results.json
		AuthzenPDPInteropResourceSearch01Test.class,
		AuthzenPDPInteropResourceSearch02Test.class,
		AuthzenPDPInteropResourceSearch03Test.class,
		AuthzenPDPInteropResourceSearch04Test.class,
		AuthzenPDPInteropResourceSearch05Test.class,
		AuthzenPDPInteropResourceSearch06Test.class,
		AuthzenPDPInteropResourceSearch07Test.class,
		AuthzenPDPInteropResourceSearch08Test.class,
		AuthzenPDPInteropResourceSearch09Test.class,
		AuthzenPDPInteropResourceSearch10Test.class,
		AuthzenPDPInteropResourceSearch11Test.class,
		AuthzenPDPInteropResourceSearch12Test.class,
		AuthzenPDPInteropResourceSearch13Test.class,
		AuthzenPDPInteropResourceSearch14Test.class,
		AuthzenPDPInteropResourceSearch15Test.class,
		AuthzenPDPInteropResourceSearch16Test.class,
		AuthzenPDPInteropResourceSearch17Test.class,
		AuthzenPDPInteropResourceSearch18Test.class
	}
)
public class AuthzenPDPInteropResourceSearchTestPlan implements TestPlan {
}
