package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-advanced-id1-test-plan",
	displayName = "FAPI2-Advanced-ID1: Authorization server test - BETA; subject to change, no certification programme yet",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2AdvancedID1TestPlan implements TestPlan {
	public static final List<Class<? extends TestModule>> testModules = List.of(
		// Normal well behaved client cases
		FAPI2BaselineID2DiscoveryEndpointVerification.class,
		FAPI2BaselineID2HappyFlow.class,
		FAPI2BaselineID2UserRejectsAuthentication.class,
		FAPI2BaselineID2EnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPI2BaselineID2EnsureAuthorizationRequestWithoutStateSuccess.class,
		FAPI2BaselineID2EnsureAuthorizationRequestWithoutNonceSuccess.class,
		FAPI2BaselineID2EnsureOtherScopeOrderSucceeds.class,
		FAPI2BaselineID2TestClaimsParameterIdentityClaims.class,

		// Possible failure case
		FAPI2BaselineID2EnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPI2BaselineID2EnsureRequestObjectWithLongNonce.class,
		FAPI2BaselineID2EnsureRequestObjectWithLongState.class,

		// Negative tests for request objects
		FAPI2BaselineID2EnsureRequestObjectWithoutExpFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutNbfFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutScopeFails.class,
		FAPI2BaselineID2StateOnlyOutsideRequestObjectNotUsed.class,
		FAPI2BaselineID2EnsureRequestObjectWithoutRedirectUriFails.class,
		FAPI2BaselineID2EnsureExpiredRequestObjectFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithBadAudFails.class,
		FAPI2BaselineID2EnsureRequestObjectWithExpOver60Fails.class,
		FAPI2BaselineID2EnsureRequestObjectWithNbfOver60Fails.class,
		FAPI2BaselineID2EnsureSignedRequestObjectWithRS256Fails.class,
		FAPI2BaselineID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPI2BaselineID2EnsureRequestObjectWithInvalidSignatureFails.class,
		FAPI2BaselineID2EnsureMatchingKeyInAuthorizationRequest.class,
		FAPI2BaselineID2EnsureUnsignedRequestAtParEndpointFails.class,

		// Negative tests for authorization request
		FAPI2BaselineID2EnsureRegisteredRedirectUri.class,
		FAPI2BaselineID2EnsureUnsignedAuthorizationRequestWithoutUsingParFails.class,
		FAPI2BaselineID2EnsureRedirectUriInAuthorizationRequest.class,
		FAPI2BaselineID2EnsureResponseTypeCodeIdTokenFails.class,

		// Negative tests for token endpoint
		FAPI2BaselineID2EnsureClientIdInTokenEndpoint.class,
		FAPI2BaselineID2EnsureHolderOfKeyRequired.class,
		FAPI2BaselineID2EnsureAuthorizationCodeIsBoundToClient.class,
		FAPI2BaselineID2AttemptReuseAuthorizationCodeAfterOneSecond.class,

		// Private key specific tests
		FAPI2BaselineID2EnsureSignedClientAssertionWithRS256Fails.class,
		FAPI2BaselineID2EnsureClientAssertionInTokenEndpoint.class,
		FAPI2BaselineID2EnsureClientAssertionWithExpIs5MinutesInPastFails.class,
		FAPI2BaselineID2EnsureClientAssertionWithWrongAudFails.class,
		FAPI2BaselineID2EnsureClientAssertionWithNoSubFails.class,

		FAPI2BaselineID2DpopNegativeTests.class,

		//Refresh token tests
		FAPI2BaselineID2RefreshToken.class,

		// OBUK specific tests
		FAPI2BaselineID2EnsureServerHandlesNonMatchingIntentId.class,
		FAPI2BaselineID2TestEssentialAcrScaClaim.class,

		// OB Brazil specific tests
		FAPI2BaselineID2BrazilEnsureBadPaymentSignatureFails.class,

		// IDMVP specific tests
		FAPI2BaselineID2IdmvpTestClaimsParameterIdTokenIdentityClaims.class,

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

		FAPI2BaselineID2PARRejectRequestUriInParAuthorizationRequest.class,

		FAPI2BaselineID2ParWithoutDuplicateParameters.class
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
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		boolean dpop = senderConstrain.equals("dpop");
		boolean signedRequest = requestMethod.equals("signed_non_repudiation");

		String certProfile = "FAPI2AdvancedID2 ";

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				if (jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: JARM is not used in UK",
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
			case "idmvp":
				if (!privateKey) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only private_key_jwt is used for IDMVP",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only signed requests are required for IDMVP",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (dpop) {
					throw new RuntimeException(String.format("Invalid configuration for %s: DPoP sender constraining is not used for IDMVP",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: JARM responses are not used for IDMVP",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				// as there's only one possible correct configuration, stop here and return just the name
				return "IDMVP";
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
		switch (requestMethod) {
			case "unsigned":
				break;
			case "signed_non_repudiation":
				certProfile += ", non-repudiation signed request";
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
