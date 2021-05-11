package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi1-advanced-final-test-plan",
	displayName = "FAPI1-Advanced-Final: Authorization server test - NOT YET COMPLETE, CERTIFICATION PROGRAM NOT YET LAUNCHED",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		// Normal well behaved client cases
		FAPI1AdvancedFinalDiscoveryEndpointVerification.class,
		FAPI1AdvancedFinal.class,
		FAPI1AdvancedFinalUserRejectsAuthentication.class,
		FAPI1AdvancedFinalEnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPI1AdvancedFinalEnsureAuthorizationRequestWithoutStateSuccess.class,
		FAPI1AdvancedFinalEnsureValidPKCESucceeds.class,
		FAPI1AdvancedFinalEnsureOtherScopeOrderSucceeds.class,

		// Possible failure case
		FAPI1AdvancedFinalEnsureResponseModeQuery.class,
		FAPI1AdvancedFinalEnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPI1AdvancedFinalEnsureRegisteredRedirectUri.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithLongNonce.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithLongState.class,

		// Negative tests for request objects
		FAPI1AdvancedFinalEnsureRequestObjectWithoutExpFails.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithoutNbfFails.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithoutScopeFails.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithoutState.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithoutNonceFails.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithoutRedirectUriFails.class,
		FAPI1AdvancedFinalEnsureExpiredRequestObjectFails.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithBadAudFails.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithExpOver60Fails.class,
		FAPI1AdvancedFinalEnsureSignedRequestObjectWithRS256Fails.class,
		FAPI1AdvancedFinalEnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPI1AdvancedFinalEnsureRequestObjectWithInvalidSignatureFails.class,
		FAPI1AdvancedFinalEnsureMatchingKeyInAuthorizationRequest.class,

		// Negative tests for authorization request
		FAPI1AdvancedFinalEnsureAuthorizationRequestWithoutRequestObjectFails.class,
		FAPI1AdvancedFinalEnsureRedirectUriInAuthorizationRequest.class,
		FAPI1AdvancedFinalEnsureResponseTypeCodeFails.class,

		// Negative tests for token endpoint
		FAPI1AdvancedFinalEnsureClientIdInTokenEndpoint.class,
		FAPI1AdvancedFinalEnsureMTLSHolderOfKeyRequired.class,
		FAPI1AdvancedFinalEnsureAuthorizationCodeIsBoundToClient.class,

		// Private key specific tests
		FAPI1AdvancedFinalEnsureSignedClientAssertionWithRS256Fails.class,
		FAPI1AdvancedFinalEnsureClientAssertionInTokenEndpoint.class,
		FAPI1AdvancedFinalEnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPI1AdvancedFinalEnsureClientAssertionWithWrongAudFails.class,
		FAPI1AdvancedFinalEnsureClientAssertionWithNoSubFails.class,

		//Refresh token tests
		FAPI1AdvancedFinalRefreshToken.class,

		// OB systems specific tests
		FAPI1AdvancedFinalEnsureServerHandlesNonMatchingIntentId.class,

		FAPI1AdvancedFinalTestEssentialAcrScaClaim.class,
		FAPI1AdvancedFinalAttemptReuseAuthorizationCodeAfter30S.class,
		FAPI1AdvancedFinalAttemptReuseAuthorizationCodeAfterOneSecond.class,

		//PAR tests
		FAPI1AdvancedFinalPARAttemptReuseRequestUri.class,
		FAPI1AdvancedFinalPARAttemptToUseExpiredRequestUri.class,
		FAPI1AdvancedFinalPARCheckAudienceForJWTClientAssertion.class,
		FAPI1AdvancedFinalPAREnsureRequestUriIsBoundToClient.class,
		FAPI1AdvancedFinalPARRejectRequestUriInParAuthorizationFormParams.class,
		FAPI1AdvancedFinalPARRejectInvalidHttpVerb.class,

		// PKCE tests
		FAPI1AdvancedFinalPAREnsurePKCERequired.class,
		FAPI1AdvancedFinalPAREnsurePKCECodeVerifierRequired.class,
		FAPI1AdvancedFinalPARIncorrectPKCECodeVerifierRejected.class,
		FAPI1AdvancedFinalPAREnsurePlainPKCERejected.class,

		// TODO: I suspect these 3 can also be used in the non-PAR case, check specs
		FAPI1AdvancedFinalPARRejectInvalidAudienceInRequestObject.class,
		FAPI1AdvancedFinalPARRejectInvalidRedirectUri.class,
		FAPI1AdvancedFinalPARRejectRequestUriInParAuthorizationRequest.class,

		FAPI1AdvancedFinalParWithoutDuplicateParameters.class

		// TODO: for PAR, we could also try passing a non-signed request to the PAR endpoint

	}
)
public class FAPI1AdvancedFinalTestPlan implements TestPlan {

}
