package net.openid.conformance.fapiciba;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi-ciba-id1-test-plan",
	displayName = "FAPI-CIBA-ID1: Authorization server test",
	profile = TestPlan.ProfileNames.optest,
	testModules = {
		// Discovery
		FAPICIBAID1DiscoveryEndpointVerification.class,
		FAPICIBABrazilDiscoveryEndpointVerification.class,

		// Normal well behaved client cases
		FAPICIBAID1.class,
		FAPICIBAID1UserRejectsAuthentication.class,
		FAPICIBAID1MultipleCallToTokenEndpoint.class,
		FAPICIBAID1AuthReqIdExpired.class,
		FAPICIBAID1EnsureAuthorizationRequestWithBindingMessageSucceeds.class,
		FAPICIBAID1EnsureOtherScopeOrderSucceeds.class,
		FAPICIBAID1EnsureRequestedExpiryAsStringSucceeds.class,

		// Possible failure case
		FAPICIBAID1EnsureAuthorizationRequestWithPotentiallyBadBindingMessage.class,

		// Negative tests for request objects
		FAPICIBAID1EnsureRequestObjectMissingAudFails.class,
		FAPICIBAID1EnsureRequestObjectBadAudFails.class,
		FAPICIBAID1EnsureRequestObjectMissingIssFails.class,
		FAPICIBAID1EnsureRequestObjectBadIssFails.class,
		FAPICIBAID1EnsureRequestObjectMissingExpFails.class,
		FAPICIBAID1EnsureRequestObjectExpiredExpFails.class,
		FAPICIBAID1EnsureRequestObjectExpIs70MinutesInFutureFails.class,
		FAPICIBAID1EnsureRequestObjectMissingIatFails.class,
		FAPICIBAID1EnsureRequestObjectMissingNbfFails.class,
		FAPICIBAID1EnsureRequestObjectNbfIs10MinutesInFutureFails.class,
		FAPICIBAID1EnsureRequestObjectNbfIs70MinutesInPastFails.class,
		FAPICIBAID1EnsureRequestObjectMissingJtiFails.class,
		FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsNoneFails.class,
		FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsBadFails.class,
		FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsRS256Fails.class,
		FAPICIBAID1EnsureRequestObjectSignedByOtherClientFails.class,

		// Negative tests for hints
		FAPICIBAID1EnsureAuthorizationRequestWithMultipleHintsFails.class,

		// Negative tests for token endpoint
		FAPICIBAID1EnsureWrongAuthenticationRequestIdInTokenEndpointRequest.class,

		// MTLS specific tests - not possible to test with private_key_jwt
		FAPICIBAID1EnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequest.class,
		FAPICIBAID1EnsureWrongClientIdInTokenEndpointRequest.class,
		FAPICIBAID1EnsureWrongClientIdInBackchannelAuthorizationRequest.class,

		// private_key_jwt specific tests - not possible to test with mtls
		FAPICIBAID1EnsureWithoutClientAssertionInTokenEndpointFails.class,
		FAPICIBAID1EnsureWithoutClientAssertionInBackchannelAuthorizationRequestFails.class,
		FAPICIBAID1EnsureClientAssertionSignatureAlgorithmInBackchannelAuthorizationRequestIsRS256Fails.class,
		FAPICIBAID1EnsureClientAssertionSignatureAlgorithmInTokenEndpointRequestIsRS256Fails.class,
		FAPICIBAID1EnsureClientAssertionWithIssAudToTokenEndpointSucceeds.class,

		// Ping specific tests, not applicable to poll
		FAPICIBAID1PingNotificationEndpointReturnsABody.class,
		FAPICIBAID1PingNotificationEndpointReturns401.class,
		FAPICIBAID1PingNotificationEndpointReturns403.class,
		FAPICIBAID1PingNotificationEndpointReturns401AndRequireServerDoesNotRetry.class,
		FAPICIBAID1PingNotificationEndpointReturnsRedirectRequest.class,

		// FAPI specific
		FAPICIBAID1EnsureBackchannelAuthorizationRequestWithoutRequestFails.class,

		//Refresh token tests
		FAPICIBAID1RefreshToken.class,

		FAPICIBAID1EnsureMTLSHolderOfKeyRequired.class,
	}
)
public class FAPICIBAID1TestPlan implements TestPlan {
	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String cibaMode = v.get("ciba_mode");
		boolean privateKey = ClientAuthType.PRIVATE_KEY_JWT.toString().equals(clientAuth);
		boolean poll = CIBAMode.POLL.toString().equals(cibaMode);

		switch (profile) {
			case "plain_fapi":
			case "openbanking_uk":
				certProfile = "FAPI-CIBA";
				break;
			case "openbanking_brazil":
				certProfile = "BR-OB-CIBA";
				if (!privateKey || !poll) {
					throw new RuntimeException("Invalid configuration for %s: Client Authentication Type must be private_key_jwt and CIBA Mode must be poll for Brazil Open Finance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case  "openinsurance_brazil":
				certProfile = "BR-OPIN-CIBA";
				if (!privateKey || !poll) {
					throw new RuntimeException("Invalid configuration for %s: Client Authentication Type must be private_key_jwt and CIBA Mode must be poll for Brazil Open Insurance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "consumerdataright_au":
			default:
				return "";	//Not a profile
		}

		certProfile += " OP ";
		switch (cibaMode) {
			case "poll":
				certProfile += "poll";
				break;
			case "ping":
				certProfile += "ping";
				break;
		}
		certProfile += " w/ ";
		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += "Private Key";
				break;
			case "mtls":
				certProfile += "MTLS";
				break;
		}

		return certProfile;
	}
}
