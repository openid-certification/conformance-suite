package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-client-test-with-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2: Relying Party (client test) using mtls client authentication",
	profile = "FAPI-RW-ID2-Relying-Party-Client-Test",
	testModules = {
		FAPIRWID2ClientTestWithMTLSHolderOfKey.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidSHash.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidCHash.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidNonce.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidIss.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidAud.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidSecondaryAud.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidSignature.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidNullAlg.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidAlternateAlg.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidExpiredExp.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidMissingExp.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyIatIsWeekInPast.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidMissingAud.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidMissingIss.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidMissingNonce.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyInvalidMissingSHash.class,
		FAPIRWID2ClientTestWithMTLSHolderOfKeyValidAudAsArray.class
	},
	variants = {
		FAPIRWID2ClientTest.variant_mtls
	}
)
public class FAPIRWID2ClientTestWithMTLSHolderOfKeyTestPlan implements TestPlan {

}
