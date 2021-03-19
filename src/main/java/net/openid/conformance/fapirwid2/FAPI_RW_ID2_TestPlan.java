package net.openid.conformance.fapirwid2;

import net.openid.conformance.par.FAPIRWID2PARAttemptReuseRequestUri;
import net.openid.conformance.par.FAPIRWID2PARAttemptToUseExpiredRequestUri;
import net.openid.conformance.par.FAPIRWID2PARCheckAudienceForJWTClientAssertion;
import net.openid.conformance.par.FAPIRWID2PAREnsureRequestUriIsBoundToClient;
import net.openid.conformance.par.FAPIRWID2PARRejectInvalidAudienceInRequestObject;
import net.openid.conformance.par.FAPIRWID2PARRejectInvalidHttpVerb;
import net.openid.conformance.par.FAPIRWID2PARRejectInvalidRedirectUri;
import net.openid.conformance.par.FAPIRWID2PARRejectRequestUriInParAuthorizationFormParams;
import net.openid.conformance.par.FAPIRWID2PARRejectRequestUriInParAuthorizationRequest;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-test-plan",
	displayName = "FAPI-RW-ID2 (and OpenBankingUK / CDR): Authorization server test (latest version)",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		// Normal well behaved client cases
		FAPIRWID2DiscoveryEndpointVerification.class,
		FAPIRWID2.class,
		FAPIRWID2UserRejectsAuthentication.class,
		FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPIRWID2EnsureAuthorizationRequestWithoutStateSuccess.class,
		FAPIRWID2EnsureValidPKCESucceeds.class,
		FAPIRWID2EnsureOtherScopeOrderSucceeds.class,

		// Possible failure case
		FAPIRWID2EnsureResponseModeQuery.class,
		FAPIRWID2EnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPIRWID2EnsureRegisteredRedirectUri.class,
		FAPIRWID2EnsureRequestObjectWithLongNonce.class,
		FAPIRWID2EnsureRequestObjectWithLongState.class,

		// Negative tests for request objects
		FAPIRWID2EnsureRequestObjectWithoutExpFails.class,
		FAPIRWID2EnsureRequestObjectWithoutScopeFails.class,
		FAPIRWID2EnsureRequestObjectWithoutState.class,
		FAPIRWID2EnsureRequestObjectWithoutNonceFails.class,
		FAPIRWID2EnsureRequestObjectWithoutRedirectUriFails.class,
		FAPIRWID2EnsureExpiredRequestObjectFails.class,
		FAPIRWID2EnsureRequestObjectWithBadAudFails.class,
		FAPIRWID2EnsureSignedRequestObjectWithRS256Fails.class,
		FAPIRWID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPIRWID2EnsureRequestObjectWithInvalidSignatureFails.class,
		FAPIRWID2EnsureMatchingKeyInAuthorizationRequest.class,

		// Negative tests for authorization request
		FAPIRWID2EnsureAuthorizationRequestWithoutRequestObjectFails.class,
		FAPIRWID2EnsureRedirectUriInAuthorizationRequest.class,
		FAPIRWID2EnsureResponseTypeCodeFails.class,

		// Negative tests for token endpoint
		FAPIRWID2EnsureClientIdInTokenEndpoint.class,
		FAPIRWID2EnsureMTLSHolderOfKeyRequired.class,
		FAPIRWID2EnsureAuthorizationCodeIsBoundToClient.class,

		// Private key specific tests
		FAPIRWID2EnsureSignedClientAssertionWithRS256Fails.class,
		FAPIRWID2EnsureClientAssertionInTokenEndpoint.class,
		FAPIRWID2EnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPIRWID2EnsureClientAssertionWithWrongAudFails.class,
		FAPIRWID2EnsureClientAssertionWithNoSubFails.class,

		//Refresh token tests
		FAPIRWID2RefreshToken.class,

		// OB systems specific tests
		FAPIRWID2EnsureServerHandlesNonMatchingIntentId.class,

		FAPIRWID2TestEssentialAcrScaClaim.class,
		FAPIRWID2AttemptReuseAuthorizationCodeAfter30S.class,
		FAPIRWID2AttemptReuseAuthorizationCodeAfterOneSecond.class,

		//PAR tests
		FAPIRWID2PARAttemptReuseRequestUri.class,
		FAPIRWID2PARAttemptToUseExpiredRequestUri.class,
		FAPIRWID2PARCheckAudienceForJWTClientAssertion.class,
		FAPIRWID2PAREnsureRequestUriIsBoundToClient.class,
		FAPIRWID2PARRejectRequestUriInParAuthorizationFormParams.class,
		FAPIRWID2PARRejectInvalidHttpVerb.class,

		// TODO: I suspect these 3 can also be used in the non-PAR case, check specs
		FAPIRWID2PARRejectInvalidAudienceInRequestObject.class,
		FAPIRWID2PARRejectInvalidRedirectUri.class,
		FAPIRWID2PARRejectRequestUriInParAuthorizationRequest.class,

		FAPIRWID2ParWithoutDuplicateParameters.class

		// TODO: for PAR, we could also try passing a non-signed request to the PAR endpoint

	}
)
public class FAPI_RW_ID2_TestPlan implements TestPlan {

}
