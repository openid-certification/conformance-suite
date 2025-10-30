package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi2-message-signing-final-client-credentials-grant-client-test-plan",
	displayName = "FAPI2-Message-Signing-Final: Relying Party (client) client credentials grant test",
	profile = TestPlan.ProfileNames.rptest
)
public class FAPI2MessageSigningFinalClientCredentialsGrantClientTestPlan implements TestPlan {
	public static final List<Class<? extends TestModule>> testModules = List.of(
		FAPI2SPFinalClientTestHappyPath.class,

		// Happy path for DPoP sender constrained without DPoP nonce
		FAPI2SPFinalClientTestHappyPathNoDpopNonce.class,
		FAPI2SPFinalClientTestTokenTypeCaseInsenstivity.class
	);

	public static List<ModuleListEntry> testModulesWithVariants() {
		List<TestPlan.Variant> baselineVariants = List.of(
			new TestPlan.Variant(AuthorizationRequestType.class, "simple"),
			new TestPlan.Variant(FAPI2AuthRequestMethod.class, "unsigned"),
			new TestPlan.Variant(FAPIResponseMode.class, "plain_response"),
			new TestPlan.Variant(FAPI2FinalOPProfile.class, "fapi_client_credentials_grant"),
			new TestPlan.Variant(FAPIClientType.class, "plain_oauth")
		);

		return List.of(
			new ModuleListEntry(testModules, baselineVariants)
		);

	}

	public static List<String> certificationProfileName(VariantSelection variant) {

		List<String> profiles = new ArrayList<>();

		Map<String, String> v = variant.getVariant();
		String clientAuth = v.get("client_auth_type");
		String senderConstrain = v.get("sender_constrain");

		String certProfile = "FAPI2SP RP ";

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
