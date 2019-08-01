package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTest;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestIatIsWeekInPast;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidAlternateAlg;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidAud;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidCHash;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidExpiredExp;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidIss;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidMissingAud;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidMissingExp;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidMissingIss;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidMissingNonce;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidMissingSHash;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidNonce;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidNullAlg;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidOpenBankingIntentId;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidSHash;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidSecondaryAud;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestInvalidSignature;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestNoAtHash;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBClientTestValidAudAsArray;
import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-client-test-plan",
	displayName = "FAPI-RW-ID2: Relying Party (client test)",
	profile = "FAPI-RW-ID2-Relying-Party-Client-Test",
	testModules = {
		FAPIRWID2ClientTest.class,
		FAPIRWID2ClientTestInvalidSHash.class,
		FAPIRWID2ClientTestInvalidCHash.class,
		FAPIRWID2ClientTestInvalidNonce.class,
		FAPIRWID2ClientTestInvalidIss.class,
		FAPIRWID2ClientTestInvalidAud.class,
		FAPIRWID2ClientTestInvalidSecondaryAud.class,
		FAPIRWID2ClientTestInvalidSignature.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidNullAlg.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidAlternateAlg.class,
		FAPIRWID2ClientTestInvalidExpiredExp.class,
		FAPIRWID2ClientTestInvalidMissingExp.class,
		FAPIRWID2ClientTestIatIsWeekInPast.class,
		FAPIRWID2ClientTestInvalidMissingAud.class,
		FAPIRWID2ClientTestInvalidMissingIss.class,
		FAPIRWID2ClientTestInvalidMissingNonce.class,
		FAPIRWID2ClientTestInvalidMissingSHash.class,
		FAPIRWID2ClientTestValidAudAsArray.class,
		FAPIRWID2ClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyInvalidNullAlg.class,
		FAPIRWID2ClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyInvalidAlternateAlg.class,
		FAPIRWID2OBClientTest.class,
		FAPIRWID2OBClientTestNoAtHash.class,
		FAPIRWID2OBClientTestInvalidSHash.class,
		FAPIRWID2OBClientTestInvalidCHash.class,
		FAPIRWID2OBClientTestInvalidNonce.class,
		FAPIRWID2OBClientTestInvalidIss.class,
		FAPIRWID2OBClientTestInvalidAud.class,
		FAPIRWID2OBClientTestInvalidSecondaryAud.class,
		FAPIRWID2OBClientTestInvalidOpenBankingIntentId.class,
		FAPIRWID2OBClientTestInvalidSignature.class,
		FAPIRWID2OBClientTestInvalidNullAlg.class,
		FAPIRWID2OBClientTestInvalidAlternateAlg.class,
		FAPIRWID2OBClientTestInvalidMissingExp.class,
		FAPIRWID2OBClientTestInvalidExpiredExp.class,
		FAPIRWID2OBClientTestIatIsWeekInPast.class,
		FAPIRWID2OBClientTestInvalidMissingAud.class,
		FAPIRWID2OBClientTestInvalidMissingIss.class,
		FAPIRWID2OBClientTestInvalidMissingNonce.class,
		FAPIRWID2OBClientTestInvalidMissingSHash.class,
		FAPIRWID2OBClientTestValidAudAsArray.class
	},
	variants = {
		FAPIRWID2ClientTest.variant_mtls,
		FAPIRWID2ClientTest.variant_privatekeyjwt,
		FAPIRWID2ClientTest.variant_openbankinguk_mtls,
		FAPIRWID2ClientTest.variant_openbankinguk_privatekeyjwt
	}
)
public class FAPIRWID2ClientTestPlan implements TestPlan {

}
