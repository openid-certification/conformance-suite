package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.fapi1advancedfinal.*;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@PublishTestPlan (
	testPlanName = "fapi-ciba-id1-client-test-plan",
	displayName = "FAPI-CIBA-ID1: Relying Party (client test)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		FAPICIBAID1ClientTest.class
	}
)
public class FAPICIBAID1ClientTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {

		String certProfile = null;

		Map<String, String> v = variant.getVariant();
		String profile = v.get("fapi_profile");
		String clientAuth = v.get("client_auth_type");
		String responseMode = v.get("fapi_response_mode");
		String jarmType = v.get("fapi_jarm_type");
		String cibaMode = v.get("ciba_mode");
		boolean jarm = responseMode.equals("jarm");
		boolean privateKey = clientAuth.equals("private_key_jwt");

		switch (profile) {
			case "plain_fapi":
				certProfile = "FAPI";
				break;
			case "openbanking_uk":
				certProfile = "UK-OB";
				if (jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: PAR/JARM are not used in UK",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "consumerdataright_au":
				certProfile = "AU-CDR";
				if (!privateKey) {
					throw new RuntimeException(String.format("Invalid configuration for %s: Only private_key_jwt is used for AU-CDR",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				if (jarm) {
					throw new RuntimeException(String.format("Invalid configuration for %s: JARM is not used in AU-CDR",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
				break;
			case "openbanking_brazil":
				return "Not a conformance profile. Please use 'FAPI1-Advanced-Final: Open Banking Brazil Relying Party (Client) Test Plan' for Brazil OB RP certification.";
		}

		certProfile += "-CIBA RP " + cibaMode;

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " w/ Private Key";
				break;
			case "mtls":
				certProfile += " w/ MTLS";
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
}
