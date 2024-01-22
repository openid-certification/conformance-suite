package net.openid.conformance.fapirwid2;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-client-test-plan",
	displayName = "FAPI-RW-ID2: Relying Party (client test) - DEPRECATED; will be removed December 2024",
	summary = "Implementer's draft 2 of FAPI1 was superceded by FAPI1 Advanced Final in March 2021. The tests will be removed in December 2024 and all implementers should switch to using the FAPI1 Advanced Final tests before then. See https://bitbucket.org/openid/fapi/issues/570/deprecation-removal-of-fapi-1-implementers",
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
	public static String certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String clientAuth = v.get("client_auth_type");

		//TODO we don't have UK or AU specific RP profile names.
		String certProfile = "FAPI";

		certProfile += " R/W RP w/";

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " Private Key";
				break;
			case "mtls":
				certProfile += " MTLS";
				break;
		}
		//TODO we don't have a PAR profile for FAPI RW ID2 RP tests
		/*
		switch (requestMethod) {
			case "by_value":
				// nothing
				break;
			case "pushed":
				certProfile += ", PAR";
				break;
		}
		 */

		return certProfile;
	}
}
