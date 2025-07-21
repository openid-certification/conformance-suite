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
	testPlanName = "fapi2-security-profile-final-test-plan",
	displayName = "FAPI2-Security-Profile-Final: Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2SPFinalTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2MessageSigningFinalTestPlan.testModules);

		// these require signing, so remove them (otherwise the VariantService gets upset on app start)
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithoutExpFails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithoutNbfFails.class);
		modules.remove(FAPI2SPFinalEnsureExpiredRequestObjectFails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithBadAudFails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithExpOver60Fails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithNbfOver60Fails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted.class);
		modules.remove(FAPI2SPFinalEnsureSignedRequestObjectWithRS256Fails.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectSignatureAlgorithmIsNotNone.class);
		modules.remove(FAPI2SPFinalEnsureRequestObjectWithInvalidSignatureFails.class);
		modules.remove(FAPI2SPFinalEnsureMatchingKeyInAuthorizationRequest.class);
		modules.remove(FAPI2SPFinalEnsureUnsignedRequestAtParEndpointFails.class);
		modules.remove(FAPI2SPFinalPARRejectRequestUriInParAuthorizationRequest.class);

		List<TestPlan.Variant> baselineVariants = List.of(
			new TestPlan.Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new TestPlan.Variant(FAPIResponseMode.class, "plain_response")
		);

		return List.of(
			new TestPlan.ModuleListEntry(modules, baselineVariants)
		);

	}

	public static List<String> certificationProfileName(VariantSelection variant) {

		List<String> profiles = new ArrayList<>();


		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String senderConstrain = v.get("sender_constrain");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		String clientType = v.get("openid");
		boolean openid = clientType.equals("openid_connect");

		if (openid) {
			profiles.add("FAPI2SP OP OpenID Connect");
		}

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				return List.of("FAPI2SP OP UK-OB OP");
			case "consumerdataright_au":
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				return List.of("FAPI2SP OP AU-CDR OP");
			case "openbanking_brazil":
				return List.of( "FAPI2SP OP BR-OB OP");
			case "connectid_au":
				throw new RuntimeException("Invalid configuration for %s: Please use the FAPI2 Message Signing test plan for ConnectID".formatted(
					MethodHandles.lookup().lookupClass().getSimpleName()));
			case "cbuae":
				throw new RuntimeException("CBUAE profile requires the usage of JAR, please use the message signing test plan.");
			default:
				throw new RuntimeException("Unknown profile %s for %s".formatted(
					profile, MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		String certProfile = "FAPI2SP OP ";

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += "private key";
				break;
			case "mtls":
				certProfile += "MTLS";
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
}
