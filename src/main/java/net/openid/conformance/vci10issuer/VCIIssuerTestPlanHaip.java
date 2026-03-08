package net.openid.conformance.vci10issuer;

import net.openid.conformance.fapi2spfinal.FAPI2SPFinalAccessTokenTypeHeaderCaseSensitivity;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalAttemptReuseAuthorizationCodeAfterOneSecond;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalAttemptToUseExpiredAuthCode;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalCheckDpopProofNbfExp;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalDiscoveryEndpointVerification;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalDpopNegativeTests;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureAuthorizationCodeIsBoundToClient;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureAuthorizationRequestWithoutStateSuccess;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureClientIdInTokenEndpoint;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureDifferentStateInsideAndOutsideRequestObject;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureDpopAuthCodeBindingSuccess;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureDpopProofAtParEndpointBindingSuccess;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureDpopProofWithIat10SecondsAfterSucceeds;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureDpopProofWithIat10SecondsBeforeSucceeds;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureHolderOfKeyRequired;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureMismatchedDpopJktFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureRedirectUriInAuthorizationRequest;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureRegisteredRedirectUri;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureRequestObjectWithLongState;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureRequestObjectWithoutRedirectUriFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureResponseTypeCodeIdTokenFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureResponseTypeTokenFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopJkt;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopProofJkt;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalEnsureUnsignedAuthorizationRequestWithoutUsingParFails;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalHappyFlow;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPARAttemptReuseRequestUri;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPARAttemptToUseExpiredRequestUri;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREnsurePKCECodeVerifierRequired;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREnsurePKCERequired;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREnsurePlainPKCERejected;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREnsureRequestUriIsBoundToClient;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPARIncorrectPKCECodeVerifierRejected;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPARRejectInvalidHttpVerb;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalPARRejectRequestUriInParAuthorizationFormParams;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalParWithoutDuplicateParameters;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalRefreshToken;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalStateOnlyOutsideRequestObjectNotUsed;
import net.openid.conformance.fapi2spfinal.FAPI2SPFinalUserRejectsAuthentication;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vci-1_0-issuer-haip-test-plan",
	displayName = "OpenID for Verifiable Credential Issuance 1.0 Final/HAIP: Test an issuer (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.vciissuer
)
public class VCIIssuerTestPlanHaip implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerMetadataTest.class,
					VCIIssuerMetadataSignedTest.class
				),
				List.of(
					new Variant(VCIProfile.class, "haip"),
					new Variant(ClientAuthType.class, "client_attestation")
				)
			),
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerHappyFlow.class,
					VCIIssuerHappyFlowAdditionalRequests.class,
					VCIIssuerHappyFlowMultipleClients.class,
					VCIIssuerHappyFlowWithSkipNotification.class,
					// negative tests
					VCIIssuerFailOnInvalidNonce.class,
					VCIIssuerFailOnReplayNonce.class,
					VCIIssuerFailOnInvalidJwtProofSignature.class,
					VCIIssuerFailOnInvalidKeyAttestationSignature.class,
					VCIIssuerFailOnInvalidClientAttestationSignature.class,
					VCIIssuerFailOnInvalidClientAttestationPopSignature.class,
					VCIIssuerFailOnMismatchedClientAttestationPopKey.class,
					VCIIssuerFailOnMissingProof.class,
					VCIIssuerFailOnUnknownCredentialConfigurationId.class,
					VCIIssuerFailOnUnknownCredentialIdentifier.class,
					VCIIssuerFailOnRequestWithAccessTokenInQuery.class
				),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(VCIProfile.class, "haip"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCICredentialEncryption.class, "plain")
				)
			),
			new ModuleListEntry(
				List.of(
					// positive tests
					VCIIssuerHappyFlow.class,
					// negative tests
					VCIIssuerFailOnUnknownCredentialConfigurationId.class,
					VCIIssuerFailOnUnsupportedEncryptionAlgorithm.class
				),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(VCIProfile.class, "haip"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCICredentialEncryption.class, "encrypted")
				)
			),
			// FAPI2 Security Profile tests - same as FAPI2SPFinalTestPlan minus signing-only,
			// profile-specific, OpenID-Connect-only, and private_key_jwt-only tests.
			// Discovery test is in its own entry as it doesn't have VCI variant parameters.
			new ModuleListEntry(
				List.of(
					FAPI2SPFinalDiscoveryEndpointVerification.class
				),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(AuthorizationRequestType.class, "simple")
				)
			),
			new ModuleListEntry(
				List.of(
					// Normal well behaved client cases
					FAPI2SPFinalHappyFlow.class,
					FAPI2SPFinalUserRejectsAuthentication.class,
					FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud.class,
					FAPI2SPFinalEnsureAuthorizationRequestWithoutStateSuccess.class,
					FAPI2SPFinalAccessTokenTypeHeaderCaseSensitivity.class,

					// DPoP tests
					FAPI2SPFinalCheckDpopProofNbfExp.class,
					FAPI2SPFinalEnsureDpopProofWithIat10SecondsBeforeSucceeds.class,
					FAPI2SPFinalEnsureDpopProofWithIat10SecondsAfterSucceeds.class,

					// DPoP Authorization Code Binding negative tests
					FAPI2SPFinalEnsureMismatchedDpopJktFails.class,
					FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopProofJkt.class,
					FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopJkt.class,
					FAPI2SPFinalEnsureDpopProofAtParEndpointBindingSuccess.class,
					FAPI2SPFinalEnsureDpopAuthCodeBindingSuccess.class,

					// Possible failure case
					FAPI2SPFinalEnsureDifferentStateInsideAndOutsideRequestObject.class,
					FAPI2SPFinalEnsureRequestObjectWithLongState.class,

					// Negative tests for request objects
					FAPI2SPFinalStateOnlyOutsideRequestObjectNotUsed.class,
					FAPI2SPFinalEnsureRequestObjectWithoutRedirectUriFails.class,

					// Negative tests for authorization request
					FAPI2SPFinalEnsureRegisteredRedirectUri.class,
					FAPI2SPFinalEnsureUnsignedAuthorizationRequestWithoutUsingParFails.class,
					FAPI2SPFinalEnsureRedirectUriInAuthorizationRequest.class,
					FAPI2SPFinalEnsureResponseTypeCodeIdTokenFails.class,
					FAPI2SPFinalEnsureResponseTypeTokenFails.class,

					// Negative tests for token endpoint
					FAPI2SPFinalEnsureClientIdInTokenEndpoint.class,
					FAPI2SPFinalEnsureHolderOfKeyRequired.class,
					FAPI2SPFinalEnsureAuthorizationCodeIsBoundToClient.class,
					FAPI2SPFinalAttemptReuseAuthorizationCodeAfterOneSecond.class,
					FAPI2SPFinalAttemptToUseExpiredAuthCode.class,

					FAPI2SPFinalDpopNegativeTests.class,

					// Refresh token tests
					FAPI2SPFinalRefreshToken.class,

					// PAR tests
					FAPI2SPFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion.class,
					FAPI2SPFinalPARAttemptReuseRequestUri.class,
					FAPI2SPFinalPARAttemptToUseExpiredRequestUri.class,
					FAPI2SPFinalPAREnsureRequestUriIsBoundToClient.class,
					FAPI2SPFinalPARRejectRequestUriInParAuthorizationFormParams.class,
					FAPI2SPFinalPARRejectInvalidHttpVerb.class,

					// PKCE tests
					FAPI2SPFinalPAREnsurePKCERequired.class,
					FAPI2SPFinalPAREnsurePKCECodeVerifierRequired.class,
					FAPI2SPFinalPARIncorrectPKCECodeVerifierRejected.class,
					FAPI2SPFinalPAREnsurePlainPKCERejected.class,

					FAPI2SPFinalParWithoutDuplicateParameters.class
				),
				List.of(
					new Variant(FAPI2FinalOPProfile.class, "vci"),
					new Variant(FAPIOpenIDConnect.class, "plain_oauth"),
					new Variant(FAPIResponseMode.class, "plain_response"),
					new Variant(FAPI2SenderConstrainMethod.class, "dpop"),
					new Variant(FAPI2AuthRequestMethod.class, "unsigned"),
					new Variant(ClientAuthType.class, "client_attestation"),
					new Variant(AuthorizationRequestType.class, "simple"),
					new Variant(VCIProfile.class, "haip"),
					new Variant(VCIGrantType.class, "authorization_code"),
					new Variant(VCICredentialEncryption.class, "plain")
				)
			)
		);
	}
}
