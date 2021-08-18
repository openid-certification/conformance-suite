package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi1-advanced-final-client-test-plan",
	displayName = "FAPI1-Advanced-Final: Relying Party (client test)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		FAPI1AdvancedFinalClientTest.class,
		FAPI1AdvancedFinalClientTestEncryptedIdToken.class,
		FAPI1AdvancedFinalClientTestIdTokenEncryptedUsingRSA15.class,
		FAPI1AdvancedFinalClientTestInvalidSHash.class,
		FAPI1AdvancedFinalClientTestInvalidCHash.class,
		FAPI1AdvancedFinalClientTestInvalidNonce.class,
		FAPI1AdvancedFinalClientTestInvalidIss.class,
		FAPI1AdvancedFinalClientTestInvalidAud.class,
		FAPI1AdvancedFinalClientTestInvalidSecondaryAud.class,
		FAPI1AdvancedFinalClientTestInvalidSignature.class,
		FAPI1AdvancedFinalClientTestInvalidNullAlg.class,
		FAPI1AdvancedFinalClientTestInvalidAlternateAlg.class,
		FAPI1AdvancedFinalClientTestInvalidExpiredExp.class,
		FAPI1AdvancedFinalClientTestInvalidMissingExp.class,
		FAPI1AdvancedFinalClientTestIatIsWeekInPast.class,
		FAPI1AdvancedFinalClientTestInvalidMissingAud.class,
		FAPI1AdvancedFinalClientTestInvalidMissingIss.class,
		FAPI1AdvancedFinalClientTestInvalidMissingNonce.class,
		FAPI1AdvancedFinalClientTestInvalidMissingSHash.class,
		FAPI1AdvancedFinalClientTestValidAudAsArray.class,
		FAPI1AdvancedFinalClientTestNoScopeInTokenEndpointResponse.class,
		FAPI1AdvancedFinalClientTestInvalidScopeInTokenEndpointResponse.class,
		// OB systems specific tests
		FAPI1AdvancedFinalClientTestNoAtHash.class,
		FAPI1AdvancedFinalClientTestInvalidOpenBankingIntentId.class,
		//Brazil specific
		FAPI1AdvancedFinalClientRefreshTokenTest.class
	}
)
public class FAPI1AdvancedFinalClientTestPlan implements TestPlan {
	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_auth_request_method");
		String responseMode = v.get("fapi_response_mode");
		String jarmType = v.get("fapi_jarm_type");
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
					throw new RuntimeException(String.format("Invalid configuration for %s: PAR/JARM are not used in UK",
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
				return "Not a conformance profile. Please use 'FAPI1-Advanced-Final: Open Banking Brazil Relying Party (Client) Test Plan' for Brazil OB RP certification.";
		}

		certProfile += " Adv. RP w/";

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
				switch(jarmType) {
					case "oidc":
						certProfile += " (OpenID Connect)";
						break;
					case "plain_oauth":
						certProfile += " (OAuth)";
						break;
					default:
						throw new RuntimeException(String.format("Invalid configuration for %s: Unexpected jarm type value: %s",
							MethodHandles.lookup().lookupClass().getSimpleName(), jarmType));
				}
				break;
		}


		return certProfile;
	}
}
