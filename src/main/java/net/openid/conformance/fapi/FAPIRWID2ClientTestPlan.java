package net.openid.conformance.fapi;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-client-test-plan",
	displayName = "FAPI-RW-ID2: Relying Party (client test)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		FAPIRWID2ClientTest.class,
		FAPIRWID2ClientTestInvalidSHash.class,
		FAPIRWID2ClientTestInvalidCHash.class,
		FAPIRWID2ClientTestInvalidNonce.class,
		FAPIRWID2ClientTestInvalidIss.class,
		FAPIRWID2ClientTestInvalidAud.class,
		FAPIRWID2ClientTestInvalidSecondaryAud.class,
		FAPIRWID2ClientTestInvalidSignature.class,
		FAPIRWID2ClientTestInvalidNullAlg.class,
		FAPIRWID2ClientTestInvalidAlternateAlg.class,
		FAPIRWID2ClientTestInvalidExpiredExp.class,
		FAPIRWID2ClientTestInvalidMissingExp.class,
		FAPIRWID2ClientTestIatIsWeekInPast.class,
		FAPIRWID2ClientTestInvalidMissingAud.class,
		FAPIRWID2ClientTestInvalidMissingIss.class,
		FAPIRWID2ClientTestInvalidMissingNonce.class,
		FAPIRWID2ClientTestInvalidMissingSHash.class,
		FAPIRWID2ClientTestValidAudAsArray.class,

		// OB systems specific tests
		FAPIRWID2ClientTestNoAtHash.class,
		FAPIRWID2ClientTestInvalidOpenBankingIntentId.class
	}
)
public class FAPIRWID2ClientTestPlan implements TestPlan {

}
