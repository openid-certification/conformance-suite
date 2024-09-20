package net.openid.conformance.fapi2spid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-message-signing-id1-test-plan",
	displayName = "FAPI2-Message-Signing-ID1: Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2MessageSigningID1TestPlan implements TestPlan {
	public static final List<Class<? extends TestModule>> testModules = List.of(
		// Normal well behaved client cases
		FAPI2SPID2DiscoveryEndpointVerification.class,
		FAPI2SPID2HappyFlow.class,
		FAPI2SPID2UserRejectsAuthentication.class,
		FAPI2SPID2EnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPI2SPID2EnsureAuthorizationRequestWithoutStateSuccess.class,
		FAPI2SPID2EnsureAuthorizationRequestWithoutNonceSuccess.class,
		FAPI2SPID2EnsureOtherScopeOrderSucceeds.class,
		FAPI2SPID2TestClaimsParameterIdentityClaims.class,
		FAPI2SPID2AccessTokenTypeHeaderCaseSensitivity.class,
		FAPI2SPID2EnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted.class,

		// DPoP tests
		FAPI2SPID2CheckDpopProofNbfExp.class,
		FAPI2SPID2EnsureDpopProofWithIat10SecondsBeforeSucceeds.class,
		FAPI2SPID2EnsureDpopProofWithIat10SecondsAfterSucceeds.class,

		// DPop Authorization Code Binding negative tests
		FAPI2SPID2EnsureMismatchedDpopJktFails.class,
		FAPI2SPID2EnsureTokenEndpointFailsWithMismatchedDpopProofJkt.class,
		FAPI2SPID2EnsureTokenEndpointFailsWithMismatchedDpopJkt.class,
		FAPI2SPID2EnsureDpopProofAtParEndpointBindingSuccess.class,
		FAPI2SPID2EnsureDpopAuthCodeBindingSuccess.class,

		// Possible failure case
		FAPI2SPID2EnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPI2SPID2EnsureDifferentStateInsideAndOutsideRequestObject.class,
		FAPI2SPID2EnsureRequestObjectWithLongNonce.class,
		FAPI2SPID2EnsureRequestObjectWithLongState.class,

		// Negative tests for request objects
		FAPI2SPID2EnsureRequestObjectWithoutExpFails.class,
		FAPI2SPID2EnsureRequestObjectWithoutNbfFails.class,
		FAPI2SPID2StateOnlyOutsideRequestObjectNotUsed.class,
		FAPI2SPID2EnsureRequestObjectWithoutRedirectUriFails.class,
		FAPI2SPID2EnsureExpiredRequestObjectFails.class,
		FAPI2SPID2EnsureRequestObjectWithBadAudFails.class,
		FAPI2SPID2EnsureRequestObjectWithExpOver60Fails.class,
		FAPI2SPID2EnsureRequestObjectWithNbfOver60Fails.class,
		FAPI2SPID2EnsureSignedRequestObjectWithRS256Fails.class,
		FAPI2SPID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPI2SPID2EnsureRequestObjectWithInvalidSignatureFails.class,
		FAPI2SPID2EnsureMatchingKeyInAuthorizationRequest.class,
		FAPI2SPID2EnsureUnsignedRequestAtParEndpointFails.class,

		// Negative tests for authorization request
		FAPI2SPID2EnsureRegisteredRedirectUri.class,
		FAPI2SPID2EnsureUnsignedAuthorizationRequestWithoutUsingParFails.class,
		FAPI2SPID2EnsureRedirectUriInAuthorizationRequest.class,
		FAPI2SPID2EnsureResponseTypeCodeIdTokenFails.class,

		// Negative tests for token endpoint
		FAPI2SPID2EnsureClientIdInTokenEndpoint.class,
		FAPI2SPID2EnsureHolderOfKeyRequired.class,
		FAPI2SPID2EnsureAuthorizationCodeIsBoundToClient.class,
		FAPI2SPID2AttemptReuseAuthorizationCodeAfterOneSecond.class,

		// Private key specific tests
		FAPI2SPID2EnsureSignedClientAssertionWithRS256Fails.class,
		FAPI2SPID2EnsureClientAssertionInTokenEndpoint.class,
		FAPI2SPID2EnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPI2SPID2EnsureClientAssertionWithWrongAudFails.class,
		FAPI2SPID2EnsureClientAssertionWithNoSubFails.class,
		FAPI2SPID2EnsureClientAssertionWithIssAudSucceeds.class,

		FAPI2SPID2DpopNegativeTests.class,

		//Refresh token tests
		FAPI2SPID2RefreshToken.class,

		// OBUK specific tests
		FAPI2SPID2EnsureServerHandlesNonMatchingIntentId.class,
		FAPI2SPID2TestEssentialAcrScaClaim.class,

		// OB Brazil specific tests
		FAPI2SPID2BrazilEnsureBadPaymentSignatureFails.class,

		// ConnectID specific tests
		FAPI2SPID2AustraliaConnectIdTestClaimsParameterIdTokenIdentityClaims.class,

		//PAR tests
		FAPI2SPID2PAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion.class,
		FAPI2SPID2PARAttemptReuseRequestUri.class,
		FAPI2SPID2PARAttemptToUseExpiredRequestUri.class,
		FAPI2SPID2PAREndpointAsAudienceForJWTClientAssertion.class,
		FAPI2SPID2PAREnsureJWTClientAssertionWithIatNbf8SecondsInTheFutureIsAccepted.class,
		FAPI2SPID2PAREnsureJWTClientAssertionWithIatNbfOver60SecondsInTheFutureFails.class,
		FAPI2SPID2PARTokenEndpointAsAudienceForJWTClientAssertion.class,
		FAPI2SPID2PARArrayAsAudienceForJWTClientAssertion.class,
		FAPI2SPID2PAREnsureRequestUriIsBoundToClient.class,
		FAPI2SPID2PARRejectRequestUriInParAuthorizationFormParams.class,
		FAPI2SPID2PARRejectInvalidHttpVerb.class,

		// PKCE tests
		FAPI2SPID2PAREnsurePKCERequired.class,
		FAPI2SPID2PAREnsurePKCECodeVerifierRequired.class,
		FAPI2SPID2PARIncorrectPKCECodeVerifierRejected.class,
		FAPI2SPID2PAREnsurePlainPKCERejected.class,

		FAPI2SPID2PARRejectRequestUriInParAuthorizationRequest.class,

		FAPI2SPID2ParWithoutDuplicateParameters.class,

		//negative private key authentication
		FAPI2SPID2PAREndpointAsArrayAudienceFails.class,
		FAPI2SPID2PAREndpointAsAudienceFails.class,
		FAPI2SPID2PARTokenEndpointAsAudienceFails.class

	);

	public static List<ModuleListEntry> testModulesWithVariants() {
		List<TestPlan.Variant> variant = List.of(
		);

		return List.of(
			new TestPlan.ModuleListEntry(testModules, variant)
		);

	}

	public static String certificationProfileName(VariantSelection variant) {

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

		String certProfile = "FAPI2MsgSigningID1 ";

		if (openid) {
			certProfile += "OpenID ";
		}

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in UK".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "consumerdataright_au":
				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				certProfile = "BR-OB";
				break;
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
				return certProfile + " ConnectID OP";
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
				return certProfile + " CBUAE OP";
			default:
				throw new RuntimeException("Unknown profile %s for %s".formatted(
					profile, MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		certProfile += " OP w/";

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " Private Key";
				break;
			case "mtls":
				certProfile += " MTLS client auth";
				break;
		}
		switch (senderConstrain) {
			case "mtls":
				certProfile += ", MTLS constrain";
				break;
			case "dpop":
				certProfile += ", DPoP";
				break;
		}
		switch (requestMethod) {
			case "unsigned":
				break;
			case "signed_non_repudiation":
				certProfile += ", JAR";
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
