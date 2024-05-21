package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi1-advanced-final-br-v1-client-test-plan",
	displayName = "FAPI1-Advanced-Final-Br-v1: Relying Party (client test) - v1 security profile tests during transition period",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		FAPI1AdvancedFinalBrV1ClientTest.class,
		FAPI1AdvancedFinalBrV1ClientTestEncryptedIdToken.class,
		FAPI1AdvancedFinalBrV1ClientTestIdTokenEncryptedUsingRSA15.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidSHash.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidCHash.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidNonce.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidIss.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidAud.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidSecondaryAud.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidSignature.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidNullAlg.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidAlternateAlg.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidExpiredExp.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidMissingExp.class,
		FAPI1AdvancedFinalBrV1ClientTestIatIsWeekInPast.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidMissingAud.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidMissingIss.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidMissingNonce.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidMissingSHash.class,
		FAPI1AdvancedFinalBrV1ClientTestValidAudAsArray.class,
		FAPI1AdvancedFinalBrV1ClientTestNoScopeInTokenEndpointResponse.class,
		// OB systems specific tests
		FAPI1AdvancedFinalBrV1ClientTestNoAtHash.class,
		FAPI1AdvancedFinalBrV1ClientTestInvalidOpenBankingIntentId.class,
		//Brazil specific
		FAPI1AdvancedFinalBrV1ClientRefreshTokenTest.class
	}
)
public class FAPI1AdvancedFinalBrV1ClientTestPlan implements TestPlan {
	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_auth_request_method");
		String responseMode = v.get("fapi_response_mode");
		String fapiClientType = v.get("fapi_client_type");
		boolean par = requestMethod.equals("pushed");
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");

		switch (profile) {
			case "openinsurance_brazil":
				break;
			default:
				throw new RuntimeException("This plan can only be used for Brazil OpenInsurance.");
		}

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
			case "openinsurance_brazil":
				// this deliberately doesn't throw an exception as we rely on this case in our CI to test the Brazil profile currently
				return "Not a conformance profile. Please use 'FAPI1-Advanced-Final-Br-v1: Open Banking Brazil Relying Party (Client) Test Plan' for Brazil OB RP certification.";
			case "openbanking_ksa":
				certProfile = "KSA-OB";
				if (!par) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only pused request is used for KSA-OB",
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: JARM is not used in KSA-OB",
							MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			default:
				throw new RuntimeException("Not a conformance profile.");
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
				switch(fapiClientType) {
					case "oidc":
						certProfile += " (OpenID Connect)";
						break;
					case "plain_oauth":
						certProfile += " (OAuth)";
						break;
					default:
						throw new RuntimeException(String.format("Invalid configuration for %s: Unexpected jarm type value: %s",
							MethodHandles.lookup().lookupClass().getSimpleName(), fapiClientType));
				}
				break;
		}


		return certProfile;
	}
}
