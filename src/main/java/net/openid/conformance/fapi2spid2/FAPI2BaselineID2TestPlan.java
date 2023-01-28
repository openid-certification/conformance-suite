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
	testPlanName = "fapi2-securityprofile-id2-test-plan",
	displayName = "FAPI2-Baseline-ID2: Authorization server test - BETA; subject to change, no certification programme yet",
	profile = TestPlan.ProfileNames.optest
)
public class FAPI2BaselineID2TestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2AdvancedID1TestPlan.testModules);

		// these require signing, so remove them (otherwise the VariantService gets upset on app start)
		modules.remove(FAPI2BaselineID2EnsureRequestObjectWithoutExpFails.class);
		modules.remove(FAPI2BaselineID2EnsureRequestObjectWithoutNbfFails.class);
		modules.remove(FAPI2BaselineID2EnsureExpiredRequestObjectFails.class);
		modules.remove(FAPI2BaselineID2EnsureRequestObjectWithBadAudFails.class);
		modules.remove(FAPI2BaselineID2EnsureRequestObjectWithExpOver60Fails.class);
		modules.remove(FAPI2BaselineID2EnsureRequestObjectWithNbfOver60Fails.class);
		modules.remove(FAPI2BaselineID2EnsureSignedRequestObjectWithRS256Fails.class);
		modules.remove(FAPI2BaselineID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class);
		modules.remove(FAPI2BaselineID2EnsureRequestObjectWithInvalidSignatureFails.class);
		modules.remove(FAPI2BaselineID2EnsureMatchingKeyInAuthorizationRequest.class);
		modules.remove(FAPI2BaselineID2EnsureUnsignedRequestAtParEndpointFails.class);
		modules.remove(FAPI2BaselineID2PARRejectRequestUriInParAuthorizationRequest.class);

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

		String certProfile = "FAPI2BaselineID2 ";

		switch (profile) {
			case "plain_fapi":
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				break;
			case "consumerdataright_au":
				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				certProfile = "BR-OB";
				break;
			case "idmvp":
				throw new RuntimeException(String.format("Invalid configuration for %s: Please use the FAPI2 Message Signing test plan for IDMVP",
					MethodHandles.lookup().lookupClass().getSimpleName()));
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

		return certProfile;
	}
}
