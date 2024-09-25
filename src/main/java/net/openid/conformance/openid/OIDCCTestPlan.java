package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ServerMetadata;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-test-plan",
	displayName = "OpenID Connect Core: Comprehensive Authorization server test (not part of certification program)",
	// This plan lists all OIDCC test modules and allows the user to run them with (almost) any variant settings they
	// want to enable them to comprehensively test their authorization server.
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCDiscoveryEndpointVerification.class,

					// Dynamic registration tests
					OIDCCRegistrationJwksUri.class,
					OIDCCRegistrationLogoUri.class,
					OIDCCRegistrationPolicyUri.class,
					OIDCCRegistrationSectorUri.class,
					OIDCCRegistrationTosUri.class,

					// Positive tests
					OIDCCServerTest.class,
					OIDCCRefreshToken.class,
					OIDCCEnsureRequestWithoutNonceSucceedsForCodeFlow.class,
					OIDCCIdTokenRS256.class,
					OIDCCIdTokenSignature.class,
					OIDCCIdTokenUnsigned.class,
					OIDCCClaimsEssential.class,
					OIDCCScopeAddress.class,
					OIDCCScopeAll.class,
					OIDCCScopeEmail.class,
					OIDCCScopePhone.class,
					OIDCCScopeProfile.class,
					OIDCEnsureOtherScopeOrderSucceeds.class,
					OIDCCPromptLogin.class,
					OIDCCPromptNoneLoggedIn.class,
					OIDCCPromptNoneNotLoggedIn.class,
					OIDCCRedirectUriQueryOK.class,
					OIDCCClaimsLocales.class,
					OIDCCIdTokenHint.class,
					OIDCCLoginHint.class,
					OIDCCMaxAge1.class,
					OIDCCMaxAge10000.class,
					OIDCCUserInfoGet.class,
					OIDCCUserInfoPostBody.class,
					OIDCCUserInfoPostHeader.class,
					OIDCCUserInfoRS256.class,
					OIDCCEnsureRequestWithAcrValuesSucceeds.class,
					OIDCCEnsureRequestWithUnknownParameterSucceeds.class,
					OIDCCUiLocales.class,
					OIDCCDisplayPage.class,
					OIDCCDisplayPopup.class,
					OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported.class,
					OIDCCRequestUriUnsigned.class,
					OIDCCRequestUriSignedRS256.class,
					OIDCCRefreshTokenRPKeyRotation.class,
					OIDCCServerRotateKeys.class,
					OIDCCEnsureRequestWithValidPkceSucceeds.class,
					OIDCCEnsureClientAssertionWithIssAudSucceeds.class,

					// negative tests
					OIDCCAuthCodeReuse.class,
					OIDCCAuthCodeReuseAfter30Seconds.class,
					OIDCCEnsureRedirectUriInAuthorizationRequest.class,
					OIDCCEnsureRegisteredRedirectUri.class,
					OIDCCEnsureRequestObjectWithRedirectUri.class,
					OIDCCEnsureRequestWithoutNonceFails.class,
					OIDCCRedirectUriQueryAdded.class,
					OIDCCRedirectUriQueryMismatch.class,
					OIDCCRedirectUriRegFrag.class,
					OIDCCRegistrationSectorBad.class,
					OIDCCResponseTypeMissing.class
				),
				List.of(
					// hardwire to discovery; some test modules are likely to fail with statically configured server
					// metadata - we really only support static for the few cases where users are trying to do the
					// minimum possible for certification (e.g. in OIDCCBasicTestPlan), in which case they would not
					// be running this test plan
					new Variant(ServerMetadata.class, "discovery")
				)
			)
		);
	}

	}
