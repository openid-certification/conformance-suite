package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vp-1final-verifier-test-plan",
	displayName = "OpenID for Verifiable Presentations 1.0 Final: Test a verifier - alpha tests (not part of certification program - use the HAIP verifier plan to certify)",
	profile = TestPlan.ProfileNames.verifierTest,
	specFamily = TestPlan.SpecFamilyNames.oid4vp,
	specVersion = TestPlan.SpecVersionNames.oid4vp1Final
)
public class VP1FinalVerifierTestPlan implements TestPlan {

	public static final List<Class<? extends TestModule>> testModules = List.of(
		// positive tests
		VP1FinalVerifierHappyFlow.class,
		VP1FinalVerifierMinimalCnfJwk.class,
		VP1FinalVerifierRequestUriMethodPost.class,
		VP1FinalVerifierRequestUriFetchedTwice.class,
		// negative tests
		VP1FinalVerifierInvalidSessionTranscript.class,
		VP1FinalVerifierInvalidKbJwtSignature.class,
		VP1FinalVerifierInvalidCredentialSignature.class,
		VP1FinalVerifierInvalidSdHash.class,
		VP1FinalVerifierInvalidKbJwtNonce.class,
		VP1FinalVerifierInvalidKbJwtAud.class,
		VP1FinalVerifierKbJwtIatInPast.class,
		VP1FinalVerifierKbJwtIatInFuture.class
	);

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(
				)
			)
		);
	}
}
