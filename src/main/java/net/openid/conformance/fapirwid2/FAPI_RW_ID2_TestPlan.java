package net.openid.conformance.fapirwid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-test-plan",
	displayName = "FAPI-RW-ID2 (and OpenBankingUK / CDR): Authorization server test - DEPRECATED; will be removed December 2024",
	summary = "Implementer's draft 2 of FAPI1 was superceded by FAPI1 Advanced Final in March 2021. The tests will be removed in December 2024 and all implementers should switch to using the FAPI1 Advanced Final tests before then. See https://bitbucket.org/openid/fapi/issues/570/deprecation-removal-of-fapi-1-implementers",
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
		FAPIRWID2EnsureClientAssertionWithIssAudSucceeds.class,

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
	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_auth_request_method");
		boolean par = requestMethod.equals("pushed");
		boolean privateKey = clientAuth.equals("private_key_jwt");

		switch (profile) {
			case "plain_fapi":
				certProfile = "FAPI";
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				if (par) {
					throw new RuntimeException("Invalid configuration for %s: PAR is not used in UK".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "consumerdataright_au":
				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			default:
				return "";	//not a profile
		}

		certProfile += " R/W OP w/";

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " Private Key";
				break;
			case "mtls":
				certProfile += " MTLS";
				break;
		}
		switch (requestMethod) {
			case "by_value":
				// nothing
				break;
			case "pushed":
				certProfile += ", PAR";
				break;
		}
		return certProfile;
	}
}
