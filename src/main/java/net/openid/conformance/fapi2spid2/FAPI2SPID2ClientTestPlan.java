package net.openid.conformance.fapi2spid2;

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
	testPlanName = "fapi2-security-profile-id2-client-test-plan",
	displayName = "FAPI2-Security-Profile-ID2: Relying Party (client) test",
	profile = TestPlan.ProfileNames.rptest
)
public class FAPI2SPID2ClientTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String senderConstrain = v.get("sender_constrain");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		String clientType = v.get("fapi_client_type");
		boolean openid = clientType.equals("oidc");

		String certProfile = "FAPI2SPID2 ";

		if (openid) {
			certProfile += "OpenID ";
		}

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				certProfile += " UK-OB";
				break;
			case "consumerdataright_au":
				certProfile += " AU-CDR";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				return "Not a conformance profile. Please use 'FAPI2-Security-Profile-ID2: Open Banking Brazil Relying Party (Client) Test Plan' for Brazil OB RP certification.";
			case "connectid_au":
				throw new RuntimeException("Invalid configuration for %s: Please use the FAPI2 Message Signing test plan for ConnectID".formatted(
					MethodHandles.lookup().lookupClass().getSimpleName()));
			case "cbuae":
				throw new RuntimeException("CBUAE profile requires the usage of JAR, please use the message signing test plan.");
			default:
				throw new RuntimeException("Unknown profile %s for %s".formatted(
					profile, MethodHandles.lookup().lookupClass().getSimpleName()));
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
		switch (senderConstrain) {
			case "mtls":
				certProfile += ", MTLS constrain";
				break;
			case "dpop":
				certProfile += ", DPoP";
				break;
		}

		return certProfile.replaceAll("  ", " ");
	}

	public static List<ModuleListEntry> testModulesWithVariants() {
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2MessageSigningID1ClientTestPlan.testModules);

		// Remove JARM tests which will cause VariantService errors on startup since this only tests response_mode=plain_response
		modules.remove(FAPI2SPID2ClientTestEnsureJarmWithoutIssFails.class);
		modules.remove(FAPI2SPID2ClientTestEnsureJarmWithInvalidIssFails.class);
		modules.remove(FAPI2SPID2ClientTestEnsureJarmWithoutAudFails.class);
		modules.remove(FAPI2SPID2ClientTestEnsureJarmWithInvalidAudFails.class);
		modules.remove(FAPI2SPID2ClientTestEnsureJarmWithoutExpFails.class);
		modules.remove(FAPI2SPID2ClientTestEnsureJarmWithExpiredExpFails.class);
		modules.remove(FAPI2SPID2ClientTestEnsureJarmWithInvalidSigFails.class);
		modules.remove(FAPI2SPID2ClientTestEnsureJarmSignatureAlgIsNotNone.class);

		List<TestPlan.Variant> variant = List.of(
			new TestPlan.Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new TestPlan.Variant(FAPIResponseMode.class, "plain_response")
		);

		return List.of(
			new TestPlan.ModuleListEntry(modules, variant)
		);

	}
}
