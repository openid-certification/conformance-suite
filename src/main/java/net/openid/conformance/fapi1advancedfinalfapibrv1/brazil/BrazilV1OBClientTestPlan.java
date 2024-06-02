package net.openid.conformance.fapi1advancedfinalfapibrv1.brazil;


import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1BrazilClientDCRHappyPathTest;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientRefreshTokenTest;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTest;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestEncryptedIdToken;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestIatIsWeekInPast;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestIdTokenEncryptedUsingRSA15;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidAlternateAlg;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidAud;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidCHash;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidExpiredExp;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidIss;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidMissingAud;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidMissingExp;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidMissingIss;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidMissingNonce;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidMissingSHash;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidNonce;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidNullAlg;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidSHash;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidSecondaryAud;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestInvalidSignature;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestNoScopeInTokenEndpointResponse;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestUnencryptedRequestObjectWithPAR;
import net.openid.conformance.fapi1advancedfinalfapibrv1.FAPI1AdvancedFinalBrV1ClientTestValidAudAsArray;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantSelection;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "fapi1-advanced-final-br-v1-brazil-client-test-plan",
	displayName = "FAPI1-Advanced-Final-Br-v1: Open Banking/Insurance Brazil Relying Party (Client) Test Plan - v1 security profile tests during transition period",
	summary = "Open Banking Brazil specific tests. " +
		"This plan requires the client to run the same set of tests twice, once passing the request object by value and once by using PAR. " +
		"Server jwks configured for this plan must contain one signing and one encryption key. " +
		"This plan requires two client configurations, jwks for the second client, which will be used for encryption tests only, " +
		"must include a key that can be used for encryption.",
	profile = TestPlan.ProfileNames.rptest
)
public class BrazilV1OBClientTestPlan implements TestPlan {
	public static String certificationProfileName(VariantSelection variant) {

		String certProfile;

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");

		switch (profile) {
			case "openinsurance_brazil":
				certProfile = "BR-OPIN Adv. RP w/";
				break;
			default:
				throw new RuntimeException("This plan can only be used for Brazil OpenInsurance.");
		}

		String clientAuth = v.get("client_auth_type");
		String responseMode = v.get("fapi_response_mode");
		String fapiClientType = v.get("fapi_client_type");

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
				throw new RuntimeException("Brazil OpenInsurance has dropped support for JARM based certification profiles.");
		}

		switch(fapiClientType) {
			case "oidc":
				break;
			default:
				throw new RuntimeException("OpenBanking and OpenInsurance RP clients are required to be oidc clients.");
		}
		return certProfile;
	}

	public static List<TestPlan.ModuleListEntry> testModulesWithVariants() {
		List<Class<? extends TestModule>> byValueModules = List.of(
			FAPI1AdvancedFinalBrV1ClientTest.class,
			FAPI1AdvancedFinalBrV1ClientTestEncryptedIdToken.class,
			FAPI1AdvancedFinalBrV1ClientTestIdTokenEncryptedUsingRSA15.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidSHash.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidCHash.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidNonce.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidIss.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidAud.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidSecondaryAud.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidSignature.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidNullAlg.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidAlternateAlg.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidExpiredExp.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidMissingExp.class,
			FAPI1AdvancedFinalBrV1ClientTestIatIsWeekInPast.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidMissingAud.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidMissingIss.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidMissingNonce.class,
			FAPI1AdvancedFinalBrV1ClientTestInvalidMissingSHash.class,
			FAPI1AdvancedFinalBrV1ClientTestValidAudAsArray.class,
			FAPI1AdvancedFinalBrV1ClientTestNoScopeInTokenEndpointResponse.class,
			FAPI1AdvancedFinalBrV1ClientRefreshTokenTest.class,
			FAPI1AdvancedFinalBrV1BrazilClientDCRHappyPathTest.class
		);
		List<Class<? extends TestModule>> parModules = new LinkedList<>();
		parModules.addAll(byValueModules);
		parModules.add(FAPI1AdvancedFinalBrV1ClientTestUnencryptedRequestObjectWithPAR.class);

		List<TestPlan.Variant> variantListByValue = List.of(
			new TestPlan.Variant(FAPIAuthRequestMethod.class, "by_value")
		);
		List<TestPlan.Variant> variantListPushed = List.of(
			new TestPlan.Variant(FAPIAuthRequestMethod.class, "pushed")
		);

		return List.of(
			new TestPlan.ModuleListEntry(byValueModules, variantListByValue),
			new TestPlan.ModuleListEntry(parModules, variantListPushed)
		);

	}
}
