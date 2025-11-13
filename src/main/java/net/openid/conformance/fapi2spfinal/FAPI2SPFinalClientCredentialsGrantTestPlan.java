package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-security-profile-final-client-credentials-grant-test-plan",
	displayName = "FAPI2-Security-Profile-Final: Authorization Client Gredentials Grant server test",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2SPFinalClientCredentialsGrantTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2MessageSigningFinalTestPlan.testModules);

		// these require signing, so remove them (otherwise the VariantService gets upset on app start)
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithoutExpFails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithoutNbfFails.class);
		modules.remove(FAPI2SPFinalEnsureExpiredRequestObjectFails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithBadAudFails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithExpOver60Fails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithNbfOver60Fails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted.class);
		modules.remove(FAPI2SPFinalEnsureSignedRequestObjectWithRS256Fails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectSignatureAlgorithmIsNotNone.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithInvalidSignatureFails.class);
		modules.remove(FAPI2SPFinalEnsureMatchingKeyInAuthorizationRequest.class);
		modules.remove(FAPI2SPFinalEnsureUnsignedRequestAtParEndpointFails.class);
		modules.remove(FAPI2SPFinalPARRejectRequestUriInParAuthorizationRequest.class);

		// these require openid_connect, so remove them (otherwise the VariantService gets upset on app start)
		modules.remove(FAPI2SPFinalEnsureAuthorizationRequestWithoutNonceSuccess.class);
		modules.remove(FAPI2SPFinalEnsureDifferentNonceInsideAndOutsideRequestObject.class);
		modules.remove(FAPI2SPFinalEnsureOtherScopeOrderSucceeds.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWith64CharNonceSuccess.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithLongNonce.class);
		modules.remove(FAPI2SPFinalTestClaimsParameterIdentityClaims.class);

		// these require a profile other than fapi_client_credentials_grant, so remove them (otherwise the VariantService gets upset on app start)
		modules.remove(FAPI2SPFinalAttemptReuseAuthorizationCodeAfterOneSecond.class);
		modules.remove(FAPI2SPFinalAttemptToUseExpiredAuthCode.class);
		modules.remove(FAPI2SPFinalAustraliaConnectIdEnsureInvalidPurposeFails.class);
		modules.remove(FAPI2SPFinalAustraliaConnectIdTestClaimsParameterIdTokenIdentityClaims.class);
		modules.remove(FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails.class);
		modules.remove(FAPI2SPFinalClientRefreshTokenTest.class);
		modules.remove(FAPI2SPFinalClientTestInvalidOpenBankingIntentId.class);
		modules.remove(FAPI2SPFinalEnsureAuthorizationCodeIsBoundToClient.class);
		modules.remove(FAPI2SPFinalEnsureAuthorizationRequestWithoutStateSuccess.class);
		modules.remove(FAPI2SPFinalEnsureDifferentStateInsideAndOutsideRequestObject.class);
		modules.remove(FAPI2SPFinalEnsureDpopAuthCodeBindingSuccess.class);
		modules.remove(FAPI2SPFinalEnsureDpopProofAtParEndpointBindingSuccess.class);
		modules.remove(FAPI2SPFinalEnsureMismatchedDpopJktFails.class);
		modules.remove(FAPI2SPFinalEnsureRedirectUriInAuthorizationRequest.class);
		modules.remove(FAPI2SPFinalEnsureRegisteredRedirectUri.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithLongState.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithoutRedirectUriFails.class);
		modules.remove(FAPI2SPFinalEnsureResponseTypeCodeIdTokenFails.class);
		modules.remove(FAPI2SPFinalEnsureResponseTypeTokenFails.class);
		modules.remove(FAPI2SPFinalEnsureServerAcceptsRequestObjectWithMultipleAud.class);
		modules.remove(FAPI2SPFinalEnsureServerHandlesNonMatchingIntentId.class);
		modules.remove(FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopJkt.class);
		modules.remove(FAPI2SPFinalEnsureTokenEndpointFailsWithMismatchedDpopProofJkt.class);
		modules.remove(FAPI2SPFinalEnsureUnsignedAuthorizationRequestWithoutUsingParFails.class);
		modules.remove(FAPI2SPFinalPARAttemptReuseRequestUri.class);
		modules.remove(FAPI2SPFinalPARAttemptToUseExpiredRequestUri.class);
		modules.remove(FAPI2SPFinalPAREndpointAsArrayAudienceFails.class);
		modules.remove(FAPI2SPFinalPAREndpointAsAudienceFails.class);
		modules.remove(FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbf8SecondsInTheFutureIsAccepted.class);
		modules.remove(FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbfOver60SecondsInTheFutureFails.class);
		modules.remove(FAPI2SPFinalPAREnsurePKCECodeVerifierRequired.class);
		modules.remove(FAPI2SPFinalPAREnsurePKCERequired.class);
		modules.remove(FAPI2SPFinalPAREnsurePlainPKCERejected.class);
		modules.remove(FAPI2SPFinalPAREnsureRequestUriIsBoundToClient.class);
		modules.remove(FAPI2SPFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion.class);
		modules.remove(FAPI2SPFinalPARIncorrectPKCECodeVerifierRejected.class);
		modules.remove(FAPI2SPFinalPARRejectInvalidHttpVerb.class);
		modules.remove(FAPI2SPFinalPARRejectRequestUriInParAuthorizationFormParams.class);
		modules.remove(FAPI2SPFinalPARTokenEndpointAsAudienceFails.class);
		modules.remove(FAPI2SPFinalParWithoutDuplicateParameters.class);
		modules.remove(FAPI2SPFinalPlainFAPIEnsureRegisteredRedirectUri.class);
		modules.remove(FAPI2SPFinalRefreshToken.class);
		modules.remove(FAPI2SPFinalStateOnlyOutsideRequestObjectNotUsed.class);
		modules.remove(FAPI2SPFinalTestEssentialAcrScaClaim.class);
		modules.remove(FAPI2SPFinalUserRejectsAuthentication.class);
		modules.remove(FAPI2SPFinalAustraliaConnectIdRequestObjectWithExpOver10Fails.class);
		modules.remove(FAPI2SPFinalAustraliaConnectIdEnsureRequestObjectWithNbfOver15Fails.class);

		List<TestPlan.Variant> baselineVariants = List.of(
			new TestPlan.Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new TestPlan.Variant(FAPIResponseMode.class, "plain_response"),
			new TestPlan.Variant(FAPI2FinalOPProfile.class, "fapi_client_credentials_grant"),
			new TestPlan.Variant(FAPIOpenIDConnect.class, "plain_oauth"),
			new TestPlan.Variant(AuthorizationRequestType.class, "simple")
		);

		return List.of(
			new TestPlan.ModuleListEntry(modules, baselineVariants)
		);

	}

	public static List<String> certificationProfileName(VariantSelection variant) {

		List<String> profiles = new ArrayList<>();

		Map<String, String> v = variant.getVariant();
		String clientAuth = v.get("client_auth_type");
		String senderConstrain = v.get("sender_constrain");

		String certProfile = "FAPI2SP OP ";

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += "private key";
				break;
			case "mtls":
				certProfile += "MTLS";
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
		return profiles;
	}
}
