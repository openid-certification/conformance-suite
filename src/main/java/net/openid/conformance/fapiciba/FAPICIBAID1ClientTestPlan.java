package net.openid.conformance.fapiciba;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan (
	testPlanName = "fapi-ciba-id1-client-test-plan",
	displayName = "FAPI-CIBA-ID1: Relying Party (client test)",
	profile = TestPlan.ProfileNames.rptest,
	testModules = {
		FAPICIBAID1ClientTest.class,
		FAPICIBAID1ClientTestNoScopeInTokenEndpointResponse.class,
		FAPICIBAID1ClientTestInvalidScopeInTokenEndpointResponse.class,
		FAPICIBAID1ClientRefreshTokenTest.class
	}
)
public class FAPICIBAID1ClientTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {

		String clientAuth = variant.getVariant().get("client_auth_type");
		String certProfile = "FAPI-CIBA ID1 RP "; // TODO poll or ping

		switch (clientAuth) {
			case "private_key_jwt":
				certProfile += " w/ Private Key";
				break;
			case "mtls":
				certProfile += " w/ MTLS";
				break;
		}

		return certProfile;
	}
}
