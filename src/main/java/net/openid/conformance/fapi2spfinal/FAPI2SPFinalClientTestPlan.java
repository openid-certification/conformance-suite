package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-security-profile-final-client-test-plan",
	displayName = "FAPI2-Security-Profile-Final: Relying Party (client) test",
	profile = TestPlan.ProfileNames.rptest,
	requireClientLog = true
)
public class FAPI2SPFinalClientTestPlan implements TestPlan {

	public static List<String> certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String senderConstrain = v.get("sender_constrain");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		String clientType = v.get("fapi_client_type");
		boolean openid = clientType.equals("oidc");
		List<String> profiles = new ArrayList<>();
		String certProfile = "FAPI2SP RP ";

		if (openid) {

			profiles.add(certProfile + "OpenID Connect");
		}

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				return List.of(certProfile + " UK-OB");
			case "consumerdataright_au":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				return List.of(certProfile + " AU-CDR");
			case "openbanking_brazil":
				throw new RuntimeException("Not a conformance profile. Please use 'FAPI2-Security-Profile-Final: Open Banking Brazil Relying Party (Client) Test Plan' for Brazil OF RP certification.");
			case "connectid_au":
				throw new RuntimeException("Invalid configuration for %s: Please use the FAPI2 Message Signing test plan for ConnectID".formatted(
					MethodHandles.lookup().lookupClass().getSimpleName()));
			case "cbuae":
				throw new RuntimeException("CBUAE profile requires the usage of JAR, please use the message signing test plan.");
			default:
				throw new RuntimeException("Unknown profile %s for %s".formatted(
					profile, MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " private key";
				break;
			case "mtls":
				certProfile += " MTLS";
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

	public static List<ModuleListEntry> testModulesWithVariants() {
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2MessageSigningFinalClientTestPlan.testModules);

		// Remove JARM tests which will cause VariantService errors on startup since this only tests response_mode=plain_response
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithoutIssFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithInvalidIssFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithoutAudFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithInvalidAudFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithoutExpFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithExpiredExpFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmWithInvalidSigFails.class);
		modules.remove(FAPI2SPFinalClientTestEnsureJarmSignatureAlgIsNotNone.class);

		List<TestPlan.Variant> variant = List.of(
			new TestPlan.Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new TestPlan.Variant(FAPIResponseMode.class, "plain_response")
		);

		return List.of(
			new TestPlan.ModuleListEntry(modules, variant)
		);

	}
}
