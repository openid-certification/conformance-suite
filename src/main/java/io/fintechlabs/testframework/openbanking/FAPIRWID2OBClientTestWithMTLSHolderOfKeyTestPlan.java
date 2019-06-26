package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2-OpenBankingUK: Relying Party (client test) using MTLS client authentication",
	profile = "FAPI-RW-ID2-OpenBankingUK-Relying-Party-Client-Test",
	testModules = {
		FAPIRWID2OBClientTestWithMTLSHolderOfKey.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyNoAtHash.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidSHash.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidCHash.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidNonce.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidIss.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidAud.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidSecondaryAud.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidOpenBankingIntentId.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidSignature.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidNullAlg.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidAlternateAlg.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidMissingExp.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidExpiredExp.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyIatIsWeekInPast.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidMissingAud.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidMissingIss.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidMissingNonce.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyInvalidMissingSHash.class,
		FAPIRWID2OBClientTestWithMTLSHolderOfKeyValidAudAsArray.class
	}
)
public class FAPIRWID2OBClientTestWithMTLSHolderOfKeyTestPlan implements TestPlan {

}
