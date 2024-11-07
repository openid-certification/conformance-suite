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
	testPlanName = "fapi2-security-profile-id2-test-plan",
	displayName = "FAPI2-Security-Profile-ID2: Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2SPID2TestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2MessageSigningID1TestPlan.testModules);

		// these require signing, so remove them (otherwise the VariantService gets upset on app start)
		modules.remove(FAPI2SPID2EnsureRequestObjectWithoutExpFails.class);
		modules.remove(FAPI2SPID2EnsureRequestObjectWithoutNbfFails.class);
		modules.remove(FAPI2SPID2EnsureExpiredRequestObjectFails.class);
		modules.remove(FAPI2SPID2EnsureRequestObjectWithBadAudFails.class);
		modules.remove(FAPI2SPID2EnsureRequestObjectWithExpOver60Fails.class);
		modules.remove(FAPI2SPID2EnsureRequestObjectWithNbfOver60Fails.class);
		modules.remove(FAPI2SPID2EnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted.class);
		modules.remove(FAPI2SPID2EnsureSignedRequestObjectWithRS256Fails.class);
		modules.remove(FAPI2SPID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class);
		modules.remove(FAPI2SPID2EnsureRequestObjectWithInvalidSignatureFails.class);
		modules.remove(FAPI2SPID2EnsureMatchingKeyInAuthorizationRequest.class);
		modules.remove(FAPI2SPID2EnsureUnsignedRequestAtParEndpointFails.class);
		modules.remove(FAPI2SPID2PARRejectRequestUriInParAuthorizationRequest.class);

		List<TestPlan.Variant> baselineVariants = List.of(
			new TestPlan.Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new TestPlan.Variant(FAPIResponseMode.class, "plain_response")
		);

		return List.of(
			new TestPlan.ModuleListEntry(modules, baselineVariants)
		);

	}

	public static String certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String senderConstrain = v.get("sender_constrain");
		boolean privateKey = clientAuth.equals("private_key_jwt");
		String clientType = v.get("openid");
		boolean openid = clientType.equals("openid_connect");

		String certProfile = "FAPI2SPID2 ";

		if (openid) {
			certProfile += "OpenID ";
		}

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				break;
			case "consumerdataright_au":
				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR".formatted(
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				certProfile = "BR-OB";
				break;
			case "connectid_au":
				throw new RuntimeException("Invalid configuration for %s: Please use the FAPI2 Message Signing test plan for ConnectID".formatted(
					MethodHandles.lookup().lookupClass().getSimpleName()));
			case "cbuae":
				throw new RuntimeException("CBUAE profile requires the usage of JAR, please use the message signing test plan.");
			default:
				throw new RuntimeException("Unknown profile %s for %s".formatted(
					profile, MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		certProfile += " OP w/";

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " Private Key";
				break;
			case "mtls":
				certProfile += " MTLS client auth";
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
}
