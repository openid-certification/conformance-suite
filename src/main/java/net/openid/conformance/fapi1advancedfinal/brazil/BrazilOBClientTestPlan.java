package net.openid.conformance.fapi1advancedfinal.brazil;


import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientRefreshTokenTest;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTest;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestEncryptedIdToken;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestIatIsWeekInPast;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestIdTokenEncryptedUsingRSA15;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidAlternateAlg;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidAud;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidCHash;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidExpiredExp;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidIss;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidMissingAud;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidMissingExp;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidMissingIss;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidMissingNonce;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidMissingSHash;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidNonce;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidNullAlg;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidSHash;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidScopeInTokenEndpointResponse;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidSecondaryAud;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestInvalidSignature;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestNoScopeInTokenEndpointResponse;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestUnencryptedRequestObjectWithPAR;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalClientTestValidAudAsArray;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "fapi1-advanced-final-brazil-client-test-plan",
	displayName = "FAPI1-Advanced-Final: Open Banking Brazil Relying Party (Client) Test Plan",
	summary = "Open Banking Brazil specific tests. " +
		"This plan requires the client to run the same set of tests twice, once passing the request object by value and once by using PAR. " +
		"Server jwks configured for this plan must contain one signing and one encryption key. " +
		"This plan requires two client configurations, jwks for the second client, which will be used for encryption tests only, " +
		"must include a key that can be used for encryption.",
	profile = TestPlan.ProfileNames.rptest
)
public class BrazilOBClientTestPlan implements TestPlan {
	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = "BR-OB Adv. RP w/";

		Map<String, String> v = variant.getVariant();
		String clientAuth = v.get("client_auth_type");
		String responseMode = v.get("fapi_response_mode");
		String jarmType = v.get("fapi_jarm_type");

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
				switch(jarmType) {
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

	public static List<TestPlan.ModuleListEntry> testModulesWithVariants() {
		List<Class<? extends TestModule>> byValueModules = List.of(
			FAPI1AdvancedFinalClientTest.class,
			FAPI1AdvancedFinalClientTestEncryptedIdToken.class,
			FAPI1AdvancedFinalClientTestIdTokenEncryptedUsingRSA15.class,
			FAPI1AdvancedFinalClientTestInvalidSHash.class,
			FAPI1AdvancedFinalClientTestInvalidCHash.class,
			FAPI1AdvancedFinalClientTestInvalidNonce.class,
			FAPI1AdvancedFinalClientTestInvalidIss.class,
			FAPI1AdvancedFinalClientTestInvalidAud.class,
			FAPI1AdvancedFinalClientTestInvalidSecondaryAud.class,
			FAPI1AdvancedFinalClientTestInvalidSignature.class,
			FAPI1AdvancedFinalClientTestInvalidNullAlg.class,
			FAPI1AdvancedFinalClientTestInvalidAlternateAlg.class,
			FAPI1AdvancedFinalClientTestInvalidExpiredExp.class,
			FAPI1AdvancedFinalClientTestInvalidMissingExp.class,
			FAPI1AdvancedFinalClientTestIatIsWeekInPast.class,
			FAPI1AdvancedFinalClientTestInvalidMissingAud.class,
			FAPI1AdvancedFinalClientTestInvalidMissingIss.class,
			FAPI1AdvancedFinalClientTestInvalidMissingNonce.class,
			FAPI1AdvancedFinalClientTestInvalidMissingSHash.class,
			FAPI1AdvancedFinalClientTestValidAudAsArray.class,
			FAPI1AdvancedFinalClientTestNoScopeInTokenEndpointResponse.class,
			FAPI1AdvancedFinalClientTestInvalidScopeInTokenEndpointResponse.class,
			FAPI1AdvancedFinalClientRefreshTokenTest.class
		);
		List<Class<? extends TestModule>> parModules = new LinkedList<>();
		parModules.addAll(byValueModules);
		parModules.add(FAPI1AdvancedFinalClientTestUnencryptedRequestObjectWithPAR.class);

		List<TestPlan.Variant> variantListByValue = List.of(
			new TestPlan.Variant(FAPI1FinalOPProfile.class, "openbanking_brazil"),
			new TestPlan.Variant(FAPIAuthRequestMethod.class, "by_value")
		);
		List<TestPlan.Variant> variantListPushed = List.of(
			new TestPlan.Variant(FAPI1FinalOPProfile.class, "openbanking_brazil"),
			new TestPlan.Variant(FAPIAuthRequestMethod.class, "pushed")
		);

		return List.of(
			new TestPlan.ModuleListEntry(byValueModules, variantListByValue),
			new TestPlan.ModuleListEntry(parModules, variantListPushed)
		);

	}
}
