package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-baseline-id2-client-test-plan",
	displayName = "FAPI2-Baseline-ID2: Relying Party (client test) - BETA; subject to change, no certification programme yet",
	profile = TestPlan.ProfileNames.rptest
)
public class FAPI2BaselineID2ClientTestPlan implements TestPlan {

	public static final List<Class<? extends TestModule>> testModules = List.of(
		FAPI2BaselineID2ClientTest.class,
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

	public static String certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String responseMode = v.get("fapi_response_mode");
		String jarmType = v.get("fapi_jarm_type");
		boolean par = true;
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");

		String certProfile = "FAPI2BaselineID2 ";

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				certProfile += " UK-OB";
				if (par || jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: PAR/JARM are not used in UK",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "consumerdataright_au":
				certProfile += " AU-CDR";
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
				return "Not a conformance profile. Please use 'FAPI2-Baseline-ID2: Open Banking Brazil Relying Party (Client) Test Plan' for Brazil OB RP certification.";
		}

		certProfile += " RP w/";

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " Private Key";
				break;
			case "mtls":
				certProfile += " MTLS";
				break;
		}
		switch (responseMode) {
			case "plain_response":
				// nothing
				break;
			case "jarm":
				certProfile += ", JARM";
				switch (jarmType) {
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

	public static List<ModuleListEntry> testModulesWithVariants() {
		List<TestPlan.Variant> variant = List.of(
		);

		return List.of(
			new TestPlan.ModuleListEntry(testModules, variant)
		);

	}
}
