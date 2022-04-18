package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-advanced-id1-client-test-plan",
	displayName = "FAPI2-Advanced-ID1: Relying Party (client) test - BETA; subject to change, no certification programme yet",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2AdvancedID1ClientTestPlan implements TestPlan {
	public static final List<Class<? extends TestModule>> testModules = List.of(
		FAPI2BaselineID2ClientTestHappyPath.class,
		FAPI2BaselineID2ClientTestInvalidIss.class,
		FAPI2BaselineID2ClientTestInvalidAud.class,
		FAPI2BaselineID2ClientTestInvalidSecondaryAud.class,
		FAPI2BaselineID2ClientTestInvalidSignature.class,
		FAPI2BaselineID2ClientTestInvalidNullAlg.class,
		FAPI2BaselineID2ClientTestInvalidAlternateAlg.class,
		FAPI2BaselineID2ClientTestInvalidExpiredExp.class,
		FAPI2BaselineID2ClientTestInvalidMissingExp.class,
		FAPI2BaselineID2ClientTestIatIsWeekInPast.class,
		FAPI2BaselineID2ClientTestInvalidMissingAud.class,
		FAPI2BaselineID2ClientTestInvalidMissingIss.class,
		FAPI2BaselineID2ClientTestValidAudAsArray.class,
		// OB systems specific tests
		FAPI2BaselineID2ClientTestInvalidOpenBankingIntentId.class,
		//Brazil specific
		FAPI2BaselineID2ClientRefreshTokenTest.class
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
