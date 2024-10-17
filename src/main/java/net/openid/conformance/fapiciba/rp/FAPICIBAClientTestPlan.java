package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi-ciba-id1-client-test-plan",
	displayName = "FAPI-CIBA-ID1: Relying Party (client test) (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		// Happy path test
		FAPICIBAClientTest.class,

		// Happy refresh token test
		FAPICIBAClientRefreshTokenTest.class,

		// Happy aud as array test
		FAPICIBAClientValidAudAsArrayTest.class,

		// Happy no scope in token endpoint + no access token expiration
		FAPICIBAClientNoScopeInTokenEndpointResponseTest.class,

		// Happy polling interval tests
		FAPICIBAClientRespectsPollingIntervalTest.class,
		FAPICIBAClientSlowDownTest.class,

		// Negative tests for backchannel endpoint response
		FAPICIBAClientBackchannelResponseMissingAuthReqIdTest.class,
		FAPICIBAClientBackchannelResponseMissingExpiresInTest.class,
		FAPICIBAClientBackchannelExpiredIdTokenHintTest.class,
		FAPICIBAClientBackchannelUnknownUserIdTest.class,
		FAPICIBAClientBackchannelInvalidIdTokenHintTest.class,

		// Negative tests for token endpoint response
		FAPICIBAClientTokenInvalidRequestTest.class,
		FAPICIBAClientTokenAccessDeniedTest.class,
		FAPICIBAClientTokenExpiredTokenTest.class,

		// Negative tests for id_token in token endpoint response
		FAPICIBAClientInvalidIssTest.class,
		FAPICIBAClientInvalidAudTest.class,
		FAPICIBAClientInvalidSecondaryAudTest.class,
		FAPICIBAClientInvalidSignatureTest.class,
		FAPICIBAClientInvalidNullAlgTest.class,
		FAPICIBAClientInvalidAlternateAlgTest.class,
		FAPICIBAClientInvalidExpiredExpTest.class,
		FAPICIBAClientInvalidIatIsWeekInPastTest.class,
		FAPICIBAClientInvalidMissingAudTest.class,
		FAPICIBAClientInvalidMissingExpTest.class,
		FAPICIBAClientInvalidMissingIssTest.class,

		// Negative test for ping mode
		FAPICIBAClientPingWithInvalidBearerTokenTest.class
	}
)
public class FAPICIBAClientTestPlan implements TestPlan {

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
				certProfile = "FAPI";
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
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
				if (!privateKey || !poll) {
					throw new RuntimeException("Invalid configuration for %s: Client Authentication Type must be private_key_jwt and CIBA Mode must be poll for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openinsurance_brazil":
				certProfile = "BR-OPIN";
				if (!privateKey || !poll) {
					throw new RuntimeException("Invalid configuration for %s: Client Authentication Type must be private_key_jwt and CIBA Mode must be poll for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;

		}

		certProfile += "-CIBA RP " + cibaMode;

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " w/ Private Key";
				break;
			case "mtls":
				certProfile += " w/ MTLS";
				break;
		}

		return certProfile;
	}
}
