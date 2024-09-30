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
		// OB systems specific tests
		FAPI1AdvancedFinalClientTestNoAtHash.class,
		FAPI1AdvancedFinalClientTestInvalidOpenBankingIntentId.class,
		//Brazil specific
		FAPI1AdvancedFinalClientRefreshTokenTest.class,
		FAPI1AdvancedFinalBrazilClientDCRHappyPathTest.class,
		FAPI1AdvancedFinalClientTestPaymentConsentRepsonseValidAudAsArray.class
	}
)
public class FAPI1AdvancedFinalClientTestPlan implements TestPlan {
	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;
		String suffix = "";

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String requestMethod = v.get("fapi_auth_request_method");
		String responseMode = v.get("fapi_response_mode");
		String fapiClientType = v.get("fapi_client_type");
		boolean par = "pushed".equals(requestMethod);
		boolean jarm = "jarm".equals(responseMode);
		boolean privateKey = "private_key_jwt".equals(clientAuth);
		boolean oidc = "oidc".equals(fapiClientType);

		switch (profile) {
			case "plain_fapi":
				certProfile = "FAPI";
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				if (par || jarm) {
					throw new RuntimeException("Invalid configuration for %s: PAR/JARM are not used in UK".formatted(
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
				certProfile = "BR-OF ";
				suffix = " (FAPI-BR v2)";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!oidc) {
					throw new RuntimeException("Invalid configuration for %s: Only client type OIDC is used for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!par) {
					throw new RuntimeException("Invalid configuration for %s: pushed authorization requests are required for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openinsurance_brazil":
				certProfile = "BR-OPIN ";
				suffix = " (FAPI-BR v2)";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!oidc) {
					throw new RuntimeException("Invalid configuration for %s: Only client type OIDC is used for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!par) {
					throw new RuntimeException("Invalid configuration for %s: pushed authorization requests are required for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used for Brazil OpenFinance".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_ksa":
				certProfile = "KSA-OB";
				if (!par) {
					throw new RuntimeException("Invalid configuration for %s: Only pushed request is used for KSA-OB".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException("Invalid configuration for %s: JARM is not used in KSA-OB".formatted(
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
				// nothing, but if you're not using JARM you must use oidc as the front channel id token is a required
				// part of FAPI1
				if (!fapiClientType.equals("oidc")) {
					throw new RuntimeException("Invalid configuration for %s: If 'plain_response' is selected, 'oidc' must be selected as the client type".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
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
						throw new RuntimeException("Invalid configuration for %s: Unexpected jarm type value: %s".formatted(
							MethodHandles.lookup().lookupClass().getSimpleName(), fapiClientType));
				}
				break;
		}


		return certProfile + suffix;
	}
}
