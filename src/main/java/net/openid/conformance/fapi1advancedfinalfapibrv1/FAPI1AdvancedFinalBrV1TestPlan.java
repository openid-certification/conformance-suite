package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "fapi1-advanced-final-br-v1-test-plan",
	displayName = "FAPI1-Advanced-Final-Br-v1: Authorization server test - v1 security profile tests during transition period, will be removed at end of transition period (mid-Oct 2024)",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		// Normal well behaved client cases
		FAPI1AdvancedFinalBrV1DiscoveryEndpointVerification.class,
		FAPI1AdvancedFinalBrV1.class,
		FAPI1AdvancedFinalBrV1UserRejectsAuthentication.class,
		FAPI1AdvancedFinalBrV1EnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPI1AdvancedFinalBrV1EnsureAuthorizationRequestWithoutStateSuccess.class,
		FAPI1AdvancedFinalBrV1EnsureValidPKCESucceeds.class,
		FAPI1AdvancedFinalBrV1EnsureOtherScopeOrderSucceeds.class,

		// Possible failure case
		FAPI1AdvancedFinalBrV1EnsureResponseModeQuery.class,
		FAPI1AdvancedFinalBrV1EnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPI1AdvancedFinalBrV1EnsureRegisteredRedirectUri.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithLongNonce.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithLongState.class,

		// Negative tests for request objects
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithoutExpFails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithoutNbfFails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithoutScopeFails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithoutState.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithoutNonceFails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithoutRedirectUriFails.class,
		FAPI1AdvancedFinalBrV1EnsureExpiredRequestObjectFails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithBadAudFails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithExpOver60Fails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithNbfOver60Fails.class,
		FAPI1AdvancedFinalBrV1EnsureSignedRequestObjectWithRS256Fails.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPI1AdvancedFinalBrV1EnsureRequestObjectWithInvalidSignatureFails.class,
		FAPI1AdvancedFinalBrV1EnsureMatchingKeyInAuthorizationRequest.class,

		// Negative tests for authorization request
		FAPI1AdvancedFinalBrV1EnsureAuthorizationRequestWithoutRequestObjectFails.class,
		FAPI1AdvancedFinalBrV1EnsureRedirectUriInAuthorizationRequest.class,
		FAPI1AdvancedFinalBrV1EnsureResponseTypeCodeFails.class,

		// Negative tests for token endpoint
		FAPI1AdvancedFinalBrV1EnsureClientIdInTokenEndpoint.class,
		FAPI1AdvancedFinalBrV1EnsureMTLSHolderOfKeyRequired.class,
		FAPI1AdvancedFinalBrV1EnsureAuthorizationCodeIsBoundToClient.class,
		FAPI1AdvancedFinalBrV1AttemptReuseAuthorizationCodeAfterOneSecond.class,

		// Private key specific tests
		FAPI1AdvancedFinalBrV1EnsureSignedClientAssertionWithRS256Fails.class,
		FAPI1AdvancedFinalBrV1EnsureClientAssertionInTokenEndpoint.class,
		FAPI1AdvancedFinalBrV1EnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPI1AdvancedFinalBrV1EnsureClientAssertionWithWrongAudFails.class,
		FAPI1AdvancedFinalBrV1EnsureClientAssertionWithNoSubFails.class,
		FAPI1AdvancedFinalBrV1EnsureClientAssertionWithIssAudSucceeds.class,

		//Refresh token tests
		FAPI1AdvancedFinalBrV1RefreshToken.class,

		// OBUK specific tests
		FAPI1AdvancedFinalBrV1EnsureServerHandlesNonMatchingIntentId.class,
		FAPI1AdvancedFinalBrV1TestEssentialAcrScaClaim.class,

		// OB Brazil specific tests
		FAPI1AdvancedFinalBrV1BrazilEnsureEncryptionRequired.class,
		FAPI1AdvancedFinalBrV1BrazilEnsureBadPaymentSignatureFails.class,

		//PAR tests
		FAPI1AdvancedFinalBrV1PARAttemptReuseRequestUri.class,
		FAPI1AdvancedFinalBrV1PARAttemptToUseExpiredRequestUri.class,
		FAPI1AdvancedFinalBrV1PARCheckAudienceForJWTClientAssertion.class,
		FAPI1AdvancedFinalBrV1PARArrayAsAudienceForJWTClientAssertion.class,
		FAPI1AdvancedFinalBrV1PAREnsureRequestUriIsBoundToClient.class,
		FAPI1AdvancedFinalBrV1PARRejectRequestUriInParAuthorizationFormParams.class,
		FAPI1AdvancedFinalBrV1PARRejectInvalidHttpVerb.class,

		// PKCE tests
		FAPI1AdvancedFinalBrV1PAREnsurePKCERequired.class,
		FAPI1AdvancedFinalBrV1PAREnsurePKCECodeVerifierRequired.class,
		FAPI1AdvancedFinalBrV1PARIncorrectPKCECodeVerifierRejected.class,
		FAPI1AdvancedFinalBrV1PAREnsurePlainPKCERejected.class,

		// TODO: I suspect these 3 can also be used in the non-PAR case, check specs
		FAPI1AdvancedFinalBrV1PARRejectInvalidAudienceInRequestObject.class,
		FAPI1AdvancedFinalBrV1PARRejectInvalidRedirectUri.class,
		FAPI1AdvancedFinalBrV1PARRejectRequestUriInParAuthorizationRequest.class,

		FAPI1AdvancedFinalBrV1ParWithoutDuplicateParameters.class

		// TODO: for PAR, we could also try passing a non-signed request to the PAR endpoint

	}
)
public class FAPI1AdvancedFinalBrV1TestPlan implements TestPlan {

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
			case "openinsurance_brazil":
				break;
			default:
				throw new RuntimeException("This plan can only be used for Brazil OpenInsurance.");
		}


		if (jarm) {
			throw new RuntimeException("Brazil OpenInsurance has dropped support for JARM based certification profiles.");
		}

		switch (profile) {
			case "plain_fapi":
				certProfile = "FAPI";
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				if (par) {
					throw new RuntimeException("Invalid configuration for %s: PAR are not used in UK".formatted(
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
				certProfile = "BR-OB";
				break;
			case "openinsurance_brazil":
				certProfile = "BR-OPIN";
				break;
			case "openbanking_ksa":
				if (!par) {
					throw new RuntimeException("Invalid configuration for %s: PAR is required for KSA OB".formatted(
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


		return certProfile;
	}
}
