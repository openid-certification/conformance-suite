package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "fapi1-advanced-final-test-plan",
	displayName = "FAPI1-Advanced-Final: Authorization server test",
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
		FAPI1AdvancedFinalAccessTokenTypeHeaderCaseSensitivity.class,

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
		FAPI1AdvancedFinalEnsureRequestObjectWithNbfOver60Fails.class,
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
		FAPI1AdvancedFinalAttemptReuseAuthorizationCodeAfterOneSecond.class,

		// Private key specific tests
		FAPI1AdvancedFinalEnsureSignedClientAssertionWithRS256Fails.class,
		FAPI1AdvancedFinalEnsureClientAssertionInTokenEndpoint.class,
		FAPI1AdvancedFinalEnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPI1AdvancedFinalEnsureClientAssertionWithWrongAudFails.class,
		FAPI1AdvancedFinalEnsureClientAssertionWithNoSubFails.class,
		FAPI1AdvancedFinalEnsureClientAssertionWithIssAudSucceeds.class,

		//Refresh token tests
		FAPI1AdvancedFinalRefreshToken.class,

		// OBUK specific tests
		FAPI1AdvancedFinalEnsureServerHandlesNonMatchingIntentId.class,
		FAPI1AdvancedFinalTestEssentialAcrScaClaim.class,

		// OB Brazil specific tests
		FAPI1AdvancedFinalBrazilEnsureEncryptionRequired.class,
		FAPI1AdvancedFinalBrazilEnsureBadPaymentSignatureFails.class,

		//PAR tests
		FAPI1AdvancedFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion.class,
		FAPI1AdvancedFinalPARAttemptReuseRequestUri.class,
		FAPI1AdvancedFinalPARAttemptToUseExpiredRequestUri.class,
		FAPI1AdvancedFinalPARCheckAudienceForJWTClientAssertion.class,
		FAPI1AdvancedFinalPARArrayAsAudienceForJWTClientAssertion.class,
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

	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;
		String suffix = "";

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
					throw new RuntimeException("Invalid configuration for %s: PAR/JARM are not used in UK".formatted(
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
			case "openbanking_brazil":
				certProfile = "BR-OF";
				suffix = " (FAPI-BR v2)";
				if (!par || jarm || !privateKey) {
					throw new RuntimeException("Invalid configuration for %s: PAR & private_key_jwt are required in Brazil OpenFinance & JARM is not used".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openinsurance_brazil":
				certProfile = "BR-OPIN";
				suffix = " (FAPI-BR v2)";
				if (!par || jarm || !privateKey) {
					throw new RuntimeException("Invalid configuration for %s: PAR & private_key_jwt are required in Brazil OpenFinance & JARM is not used".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_ksa":
				if (!par) {
					throw new RuntimeException("Invalid configuration for %s: PAR is required for KSA OB".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in KSA OB".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				certProfile = "KSA-OB";
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


		return certProfile + suffix;
	}
}
