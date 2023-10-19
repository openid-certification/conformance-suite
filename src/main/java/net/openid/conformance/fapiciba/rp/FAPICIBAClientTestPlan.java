package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi-ciba-id1-client-test-plan",
	displayName = "FAPI-CIBA-ID1: Relying Party (client test) (alpha - INCOMPLETE/INCORRECT, please email certification team if intererested)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		FAPICIBAClientTest.class,
		FAPICIBAClientRefreshTokenTest.class,

		FAPICIBAClientRespectsPollingIntervalTest.class,
		FAPICIBAClientSlowDownTest.class,

		FAPICIBAClientBackchannelResponseWithoutAuthReqIdTest.class,
		FAPICIBAClientBackchannelResponseWithoutExpiresInTest.class,
		FAPICIBAClientBackchannelExpiredIdTokenHintTest.class,
		FAPICIBAClientBackchannelUnknownUserIdTest.class,
		FAPICIBAClientBackchannelInvalidIdTokenHintTest.class,

		// Ping
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
		boolean privateKey = clientAuth.equals("private_key_jwt");

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
					throw new RuntimeException(String.format("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				certProfile = "BR-OB";
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
