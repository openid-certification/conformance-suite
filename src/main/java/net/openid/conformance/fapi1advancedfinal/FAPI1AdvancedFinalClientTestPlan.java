package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi1-advanced-final-client-test-plan",
	displayName = "FAPI1-Advanced-Final: Relying Party (client test)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
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
		// OB systems specific tests
		FAPI1AdvancedFinalClientTestNoAtHash.class,
		FAPI1AdvancedFinalClientTestInvalidOpenBankingIntentId.class
	}
)
public class FAPI1AdvancedFinalClientTestPlan implements TestPlan {

}
