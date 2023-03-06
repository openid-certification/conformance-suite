package net.openid.conformance.fapi2spid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-message-signing-id1-client-test-plan",
	displayName = "FAPI2-Message-Signing-ID1: Relying Party (client) test - BETA; subject to change, no certification programme yet",
	profile = TestPlan.ProfileNames.rptest
)
public class FAPI2MessageSigningID1ClientTestPlan implements TestPlan {
	public static final List<Class<? extends TestModule>> testModules = List.of(
		FAPI2SPID2ClientTestHappyPath.class,
		FAPI2SPID2ClientTestInvalidIss.class,
		FAPI2SPID2ClientTestInvalidAud.class,
		FAPI2SPID2ClientTestInvalidSecondaryAud.class,
		FAPI2SPID2ClientTestInvalidSignature.class,
		FAPI2SPID2ClientTestInvalidNullAlg.class,
		FAPI2SPID2ClientTestInvalidAlternateAlg.class,
		FAPI2SPID2ClientTestInvalidExpiredExp.class,
		FAPI2SPID2ClientTestInvalidMissingExp.class,
		FAPI2SPID2ClientTestInvalidMissingAud.class,
		FAPI2SPID2ClientTestInvalidMissingIss.class,
		FAPI2SPID2ClientTestValidAudAsArray.class,
		FAPI2SPID2ClientTestInvalidNonce.class,
		FAPI2SPID2ClientTestInvalidMissingNonce.class,
		FAPI2SPID2ClientTestInvalidAuthorizationResponseIss.class,
		FAPI2SPID2ClientTestRemoveAuthorizationResponseIss.class,
		FAPI2SPID2ClientTestEnsureAuthorizationResponseWithInvalidStateFails.class,
		FAPI2SPID2ClientTestEnsureAuthorizationResponseWithInvalidMissingStateFails.class,
		FAPI2SPID2ClientTestTokenEndpointResponseWithoutExpiresIn.class,
		FAPI2SPID2ClientTestTokenTypeCaseInsenstivity.class,

		// JARM tests
		FAPI2SPID2ClientTestEnsureJarmWithoutIssFails.class,
		FAPI2SPID2ClientTestEnsureJarmWithInvalidIssFails.class,
		FAPI2SPID2ClientTestEnsureJarmWithoutAudFails.class,
		FAPI2SPID2ClientTestEnsureJarmWithInvalidAudFails.class,
		FAPI2SPID2ClientTestEnsureJarmWithoutExpFails.class,
		FAPI2SPID2ClientTestEnsureJarmWithExpiredExpFails.class,
		FAPI2SPID2ClientTestEnsureJarmWithInvalidSigFails.class,
		FAPI2SPID2ClientTestEnsureJarmSignatureAlgIsNotNone.class,

		// OB systems specific tests
		FAPI2SPID2ClientTestInvalidOpenBankingIntentId.class,
		//Brazil specific
		FAPI2SPID2ClientRefreshTokenTest.class
	);

	public static List<ModuleListEntry> testModulesWithVariants() {
		List<Variant> variant = List.of(
		);

		return List.of(
			new ModuleListEntry(testModules, variant)
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

		String certProfile = "FAPI2MsgSigningID1 ";

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
			case "connectid_au":
				if (!privateKey) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only private_key_jwt is used for ConnectID",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (!signedRequest) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only signed requests are required for ConnectID",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (dpop) {
					throw new RuntimeException(String.format("Invalid configuration for %s: DPoP sender constraining is not used for ConnectID",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: JARM responses are not used for ConnectID",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				// as there's only one possible correct configuration, stop here and return just the name
				return "ConnectID RP";
		}

		certProfile += " RP w/";

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
