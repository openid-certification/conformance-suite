package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-message-signing-final-test-plan",
	displayName = "FAPI2-Message-Signing-Final: Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2MessageSigningFinalTestPlan implements TestPlan {
	public static final List<Class<? extends TestModule>> testModules = List.of(
		// Normal well behaved client cases
		FAPI2SPFinalDiscoveryEndpointVerification.class,
		FAPI2SPFinalHappyFlow.class,
		FAPI2SPFinalUserRejectsAuthentication.class,
		FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPI2SPFinalEnsureAuthorizationRequestWithoutStateSuccess.class,
		FAPI2SPFinalEnsureAuthorizationRequestWithoutNonceSuccess.class,
		FAPI2SPFinalEnsureRequestObjectWith64CharNonceSuccess.class,
		FAPI2SPFinalEnsureOtherScopeOrderSucceeds.class,
		FAPI2SPFinalTestClaimsParameterIdentityClaims.class,
		FAPI2SPFinalAccessTokenTypeHeaderCaseSensitivity.class,
		FAPI2SPFinalEnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted.class,

		// DPoP tests
		FAPI2SPFinalCheckDpopProofNbfExp.class,
		FAPI2SPFinalEnsureDpopProofWithIat10SecondsBeforeSucceeds.class,
		FAPI2SPFinalEnsureDpopProofWithIat10SecondsAfterSucceeds.class,

		// DPop Authorization Code Binding negative tests
		FAPI2SPFinalEnsureMismatchedDpopJktFails.class,
		FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopProofJkt.class,
		FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopJkt.class,
		FAPI2SPFinalEnsureDpopProofAtParEndpointBindingSuccess.class,
		FAPI2SPFinalEnsureDpopAuthCodeBindingSuccess.class,

		// Possible failure case
		FAPI2SPFinalEnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPI2SPFinalEnsureDifferentStateInsideAndOutsideRequestObject.class,
		FAPI2SPFinalEnsureRequestObjectWithLongNonce.class,
		FAPI2SPFinalEnsureRequestObjectWithLongState.class,

		// Negative tests for request objects
		FAPI2SPFinalEnsureRequestObjectWithoutExpFails.class,
		FAPI2SPFinalEnsureRequestObjectWithoutNbfFails.class,
		FAPI2SPFinalStateOnlyOutsideRequestObjectNotUsed.class,
		FAPI2SPFinalEnsureRequestObjectWithoutRedirectUriFails.class,
		FAPI2SPFinalEnsureExpiredRequestObjectFails.class,
		FAPI2SPFinalEnsureRequestObjectWithBadAudFails.class,
		FAPI2SPFinalEnsureRequestObjectWithExpOver60Fails.class,
		FAPI2SPFinalEnsureRequestObjectWithNbfOver60Fails.class,
		FAPI2SPFinalEnsureSignedRequestObjectWithRS256Fails.class,
		FAPI2SPFinalEnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPI2SPFinalEnsureRequestObjectWithInvalidSignatureFails.class,
		FAPI2SPFinalEnsureMatchingKeyInAuthorizationRequest.class,
		FAPI2SPFinalEnsureUnsignedRequestAtParEndpointFails.class,

		// Negative tests for authorization request
		FAPI2SPFinalEnsureRegisteredRedirectUri.class,
		FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri.class,
		FAPI2SPFinalEnsureUnsignedAuthorizationRequestWithoutUsingParFails.class,
		FAPI2SPFinalEnsureRedirectUriInAuthorizationRequest.class,
		FAPI2SPFinalEnsureResponseTypeCodeIdTokenFails.class,
		FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails.class,
		FAPI2SPFinalEnsureResponseTypeTokenFails.class,

		// Negative tests for token endpoint
		FAPI2SPFinalEnsureClientIdInTokenEndpoint.class,
		FAPI2SPFinalEnsureHolderOfKeyRequired.class,
		FAPI2SPFinalEnsureAuthorizationCodeIsBoundToClient.class,
		FAPI2SPFinalAttemptReuseAuthorizationCodeAfterOneSecond.class,
		FAPI2SPFinalAttemptToUseExpiredAuthCode.class,

		// Private key specific tests
		FAPI2SPFinalEnsureSignedClientAssertionWithRS256Fails.class,
		FAPI2SPFinalEnsureClientAssertionInTokenEndpoint.class,
		FAPI2SPFinalEnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPI2SPFinalEnsureClientAssertionWithWrongAudFails.class,
		FAPI2SPFinalEnsureClientAssertionWithNoSubFails.class,

		FAPI2SPFinalDpopNegativeTests.class,

		//Refresh token tests
		FAPI2SPFinalRefreshToken.class,

		// OBUK specific tests
		FAPI2SPFinalEnsureServerHandlesNonMatchingIntentId.class,
		FAPI2SPFinalTestEssentialAcrScaClaim.class,

		// OB Brazil specific tests
		FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails.class,

		// ConnectID specific tests
		FAPI2SPFinalAustraliaConnectIdTestClaimsParameterIdTokenIdentityClaims.class,

		//PAR tests
		FAPI2SPFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion.class,
		FAPI2SPFinalPARAttemptReuseRequestUri.class,
		FAPI2SPFinalPARAttemptToUseExpiredRequestUri.class,
		FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbf8SecondsInTheFutureIsAccepted.class,
		FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbfOver60SecondsInTheFutureFails.class,
		FAPI2SPFinalPAREnsureRequestUriIsBoundToClient.class,
		FAPI2SPFinalPARRejectRequestUriInParAuthorizationFormParams.class,
		FAPI2SPFinalPARRejectInvalidHttpVerb.class,

		// PKCE tests
		FAPI2SPFinalPAREnsurePKCERequired.class,
		FAPI2SPFinalPAREnsurePKCECodeVerifierRequired.class,
		FAPI2SPFinalPARIncorrectPKCECodeVerifierRejected.class,
		FAPI2SPFinalPAREnsurePlainPKCERejected.class,

		FAPI2SPFinalPARRejectRequestUriInParAuthorizationRequest.class,

		FAPI2SPFinalParWithoutDuplicateParameters.class,

		//negative private key authentication
		FAPI2SPFinalPAREndpointAsArrayAudienceFails.class,
		FAPI2SPFinalPAREndpointAsAudienceFails.class,
		FAPI2SPFinalPARTokenEndpointAsAudienceFails.class

	);

	public static List<ModuleListEntry> testModulesWithVariants() {
		List<TestPlan.Variant> variant = List.of(
		);

		return List.of(
			new TestPlan.ModuleListEntry(testModules, variant)
		);

	}

	public static List<String> certificationProfileName(VariantSelection variant) {

		List<String> profiles = new ArrayList<>();
		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_request_method");
		String responseMode = v.get("fapi_response_mode");
		String senderConstrain = v.get("sender_constrain");
		String authRequestType = v.get("authorization_request_type");
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		boolean dpop = senderConstrain.equals("dpop");
		boolean mtlsBounded = senderConstrain.equals("mtls");
		boolean signedRequest = requestMethod.equals("signed_non_repudiation");
		String clientType = v.get("openid");
		boolean openid = clientType.equals("openid_connect");
		boolean rar = "rar".equals(authRequestType);

		String certProfile = "FAPI2SP OP";

		if (openid) {
			profiles.add("FAPI2SP OP OpenID Connect");
		}

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in UK".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				return List.of( "FAPI2MS OP UK-OB");
			case "consumerdataright_au":
//				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				return List.of( "FAPI2MS OP AU-CDR");
			case "openbanking_brazil":
				return List.of( "FAPI2MS OP BR-OF");
			case "connectid_au":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are required for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (dpop) {
					throw new RuntimeException("Invalid configuration for %s: DPoP sender constraining is not used for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM responses are not used for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!openid) {
					throw new RuntimeException("Invalid configuration for %s: OpenID must be selected for ConnectID".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				// as there's only one possible correct configuration, stop here and return just the name
				return List.of("FAPI2MS OP with ConnectId support");
			case "cbuae":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are supported for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!rar) {
					throw new RuntimeException("Invalid configuration for %s: Only signed requests are supported for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!mtlsBounded) {
					throw new RuntimeException("Invalid configuration for %s: Only MTLS sender constraining is supported for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM responses are not used for CBUAE".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}

				if (!openid) {
					throw new RuntimeException(String.format("Invalid configuration for %s: OpenID must be selected for CBUAE",
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				// as there's only one possible correct configuration, stop here and return just the name
				return List.of("FAPI2MS OP CBUAE");
			default:
				throw new RuntimeException("Unknown profile %s for %s".formatted(
					profile, MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " private key";
				break;
			case "mtls":
				certProfile += " MTLS";
				break;
		}
		switch (senderConstrain) {
			case "mtls":
				certProfile += " + MTLS";
				break;
			case "dpop":
				certProfile += " + DPoP";
				break;
		}
		profiles.add(certProfile);

		switch (requestMethod) {
			case "unsigned":
				break;
			case "signed_non_repudiation":
				profiles.add("FAPI2MS OP JAR");
				break;
		}
		switch (responseMode) {
			case "plain_response":
				// nothing
				break;
			case "jarm":
				profiles.add("FAPI2MS OP JARM");
				break;
		}


		return profiles;
	}
}
