package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-baseline-id2-test-plan",
	displayName = "FAPI2-Baseline-ID2: Authorization server test - INCORRECT/INCOMPLETE, DO NOT USE",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		// Normal well behaved client cases
		FAPI2BaselineID2DiscoveryEndpointVerification.class,
		FAPI2BaselineID2.class,
		FAPI2BaselineID2UserRejectsAuthentication.class,
		FAPI2BaselineID2EnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPI2BaselineID2EnsureAuthorizationRequestWithoutStateSuccess.class,
		FAPI2BaselineID2EnsureValidPKCESucceeds.class,
		FAPI2BaselineID2EnsureOtherScopeOrderSucceeds.class,

		// Possible failure case
		FAPI2BaselineID2EnsureResponseModeQuery.class,
		FAPI2BaselineID2EnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPI2BaselineID2EnsureRegisteredRedirectUri.class,
		FAPI2BaselineID2EnsureRequestObjectWithLongNonce.class,
		FAPI2BaselineID2EnsureRequestObjectWithLongState.class,

		// Negative tests for request objects
		FAPI2BaselineID2EnsureRequestObjectWithoutExpFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutNbfFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutScopeFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutState.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutNonceFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutRedirectUriFails.class,
		FAPI2BaselineID2EnsureExpiredRequestObjectFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithBadAudFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithExpOver60Fails.class,
		FAPI2BaselineID2EnsureRequestObjectWithNbfOver60Fails.class,
		FAPI2BaselineID2EnsureSignedRequestObjectWithRS256Fails.class,
		FAPI2BaselineID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPI2BaselineID2EnsureRequestObjectWithInvalidSignatureFails.class,
		FAPI2BaselineID2EnsureMatchingKeyInAuthorizationRequest.class,

		// Negative tests for authorization request
		FAPI2BaselineID2EnsureAuthorizationRequestWithoutRequestObjectFails.class,
		FAPI2BaselineID2EnsureRedirectUriInAuthorizationRequest.class,
		FAPI2BaselineID2EnsureResponseTypeCodeFails.class,

		// Negative tests for token endpoint
		FAPI2BaselineID2EnsureClientIdInTokenEndpoint.class,
		FAPI2BaselineID2EnsureMTLSHolderOfKeyRequired.class,
		FAPI2BaselineID2EnsureAuthorizationCodeIsBoundToClient.class,
		FAPI2BaselineID2AttemptReuseAuthorizationCodeAfterOneSecond.class,

		// Private key specific tests
		FAPI2BaselineID2EnsureSignedClientAssertionWithRS256Fails.class,
		FAPI2BaselineID2EnsureClientAssertionInTokenEndpoint.class,
		FAPI2BaselineID2EnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPI2BaselineID2EnsureClientAssertionWithWrongAudFails.class,
		FAPI2BaselineID2EnsureClientAssertionWithNoSubFails.class,

		//Refresh token tests
		FAPI2BaselineID2RefreshToken.class,

		// OBUK specific tests
		FAPI2BaselineID2EnsureServerHandlesNonMatchingIntentId.class,
		FAPI2BaselineID2TestEssentialAcrScaClaim.class,

		// OB Brazil specific tests
		FAPI2BaselineID2BrazilEnsureEncryptionRequired.class,
		FAPI2BaselineID2BrazilEnsureBadPaymentSignatureFails.class,

		//PAR tests
		FAPI2BaselineID2PARAttemptReuseRequestUri.class,
		FAPI2BaselineID2PARAttemptToUseExpiredRequestUri.class,
		FAPI2BaselineID2PARCheckAudienceForJWTClientAssertion.class,
		FAPI2BaselineID2PAREnsureRequestUriIsBoundToClient.class,
		FAPI2BaselineID2PARRejectRequestUriInParAuthorizationFormParams.class,
		FAPI2BaselineID2PARRejectInvalidHttpVerb.class,

		// PKCE tests
		FAPI2BaselineID2PAREnsurePKCERequired.class,
		FAPI2BaselineID2PAREnsurePKCECodeVerifierRequired.class,
		FAPI2BaselineID2PARIncorrectPKCECodeVerifierRejected.class,
		FAPI2BaselineID2PAREnsurePlainPKCERejected.class,

		// TODO: I suspect these 3 can also be used in the non-PAR case, check specs
		FAPI2BaselineID2PARRejectInvalidAudienceInRequestObject.class,
		FAPI2BaselineID2PARRejectInvalidRedirectUri.class,
		FAPI2BaselineID2PARRejectRequestUriInParAuthorizationRequest.class,

		FAPI2BaselineID2ParWithoutDuplicateParameters.class

		// TODO: for PAR, we could also try passing a non-signed request to the PAR endpoint

	}
)
public class FAPI2BaselineID2TestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_auth_request_method");
		String responseMode = v.get("fapi_response_mode");
		boolean par = requestMethod.equals("pushed");
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");

		switch (profile) {
			case "plain_fapi":
				certProfile = "FAPI";
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				if (par || jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: PAR/JARM are not used in UK",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "consumerdataright_au":
				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: JARM is not used in AU-CDR",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				certProfile = "BR-OB";
				break;
		}

		certProfile += " Adv. OP w/";

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
		switch (responseMode) {
			case "plain_response":
				// nothing
				break;
			case "jarm":
				certProfile += ", JARM";
				break;
		}


		return certProfile;
	}
}
