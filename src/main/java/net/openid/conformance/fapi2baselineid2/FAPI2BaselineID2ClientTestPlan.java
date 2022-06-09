package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPIJARMType;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-baseline-id2-client-test-plan",
	displayName = "FAPI2-Baseline-ID2: Relying Party (client) test - BETA; subject to change, no certification programme yet",
	profile = TestPlan.ProfileNames.rptest
)
public class FAPI2BaselineID2ClientTestPlan implements TestPlan {

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
				certProfile += " UK-OB";
				break;
			case "consumerdataright_au":
				certProfile += " AU-CDR";
				if (!privateKey) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				return "Not a conformance profile. Please use 'FAPI2-Baseline-ID2: Open Banking Brazil Relying Party (Client) Test Plan' for Brazil OB RP certification.";
			case "idmvp":
				throw new RuntimeException(String.format("Invalid configuration for %s: Please use the fapi2-advanced test plan for IDMVP",
					MethodHandles.lookup().lookupClass().getSimpleName()));
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

		return certProfile;
	}

	public static List<ModuleListEntry> testModulesWithVariants() {
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2AdvancedID1ClientTestPlan.testModules);

		// Remove JARM tests which will cause VariantService errors on startup since this only tests response_mode=plain_response
		modules.remove(FAPI2BaselineID2ClientTestEnsureJarmWithoutIssFails.class);
		modules.remove(FAPI2BaselineID2ClientTestEnsureJarmWithInvalidIssFails.class);
		modules.remove(FAPI2BaselineID2ClientTestEnsureJarmWithoutAudFails.class);
		modules.remove(FAPI2BaselineID2ClientTestEnsureJarmWithInvalidAudFails.class);

		List<TestPlan.Variant> variant = List.of(
			new TestPlan.Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new TestPlan.Variant(FAPIResponseMode.class, "plain_response")
		);

		return List.of(
			new TestPlan.ModuleListEntry(modules, variant)
		);

	}
}
