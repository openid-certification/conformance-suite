package net.openid.conformance.fapi2baselineid2.brazil;


import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2BrazilClientDCRHappyPathTest;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientRefreshTokenTest;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTest;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestEncryptedIdToken;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestIatIsWeekInPast;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestIdTokenEncryptedUsingRSA15;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidAlternateAlg;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidAud;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidCHash;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidExpiredExp;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidIss;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidMissingAud;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidMissingExp;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidMissingIss;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidMissingNonce;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidMissingSHash;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidNonce;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidNullAlg;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidSHash;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidScopeInTokenEndpointResponse;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidSecondaryAud;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidSignature;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestNoScopeInTokenEndpointResponse;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestUnencryptedRequestObjectWithPAR;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestValidAudAsArray;
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
	testPlanName = "fapi2-baseline-id2-brazil-client-test-plan",
	displayName = "FAPI2-Baseline-ID2: Open Banking Brazil Relying Party (Client) Test Plan - INCORRECT/INCOMPLETE, DO NOT USE",
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
			FAPI2BaselineID2ClientTest.class,
			FAPI2BaselineID2ClientTestEncryptedIdToken.class,
			FAPI2BaselineID2ClientTestIdTokenEncryptedUsingRSA15.class,
			FAPI2BaselineID2ClientTestInvalidSHash.class,
			FAPI2BaselineID2ClientTestInvalidCHash.class,
			FAPI2BaselineID2ClientTestInvalidNonce.class,
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
			FAPI2BaselineID2ClientTestInvalidMissingNonce.class,
			FAPI2BaselineID2ClientTestInvalidMissingSHash.class,
			FAPI2BaselineID2ClientTestValidAudAsArray.class,
			FAPI2BaselineID2ClientTestNoScopeInTokenEndpointResponse.class,
			FAPI2BaselineID2ClientTestInvalidScopeInTokenEndpointResponse.class,
			FAPI2BaselineID2ClientRefreshTokenTest.class,
			FAPI2BaselineID2BrazilClientDCRHappyPathTest.class
		);
		List<Class<? extends TestModule>> parModules = new LinkedList<>();
		parModules.addAll(byValueModules);
		parModules.add(FAPI2BaselineID2ClientTestUnencryptedRequestObjectWithPAR.class);

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
