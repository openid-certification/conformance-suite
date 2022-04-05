package net.openid.conformance.fapi2baselineid2.brazil;


import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2BrazilClientDCRHappyPathTest;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestInvalidOpenBankingIntentId;
import net.openid.conformance.fapi2baselineid2.FAPI2BaselineID2ClientTestPlan;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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
		ArrayList<Class<? extends TestModule>> modules = new ArrayList<>(FAPI2BaselineID2ClientTestPlan.testModules);

		// this is marked with VariantNotApplicable for Brazil, we must remove it otherwise we get a startup error
		modules.remove(FAPI2BaselineID2ClientTestInvalidOpenBankingIntentId.class);

		modules.add(FAPI2BaselineID2BrazilClientDCRHappyPathTest.class);

		List<TestPlan.Variant> brazilVariant = List.of(
			new TestPlan.Variant(FAPI2ID2OPProfile.class, "openbanking_brazil")
		);

		return List.of(
			new TestPlan.ModuleListEntry(modules, brazilVariant)
		);

	}
}
