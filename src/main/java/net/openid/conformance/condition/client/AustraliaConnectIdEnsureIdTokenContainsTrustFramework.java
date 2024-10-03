package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AustraliaConnectIdEnsureIdTokenContainsTrustFramework extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"id_token"} )
	public Environment evaluate(Environment env) {

		JsonElement element = env.getElementFromObject("id_token", "claims.verified_claims.verification.trust_framework");

		if (element == null) {
			throw error("id_token does not contain a verification trust_framework claim.",
					args("expected", AustraliaConnectIdCheckTrustFrameworkSupported.ConnectIdTrustFramework));
		}

		String idTokenTrustFramework = OIDFJSON.getString(element);

		if (! idTokenTrustFramework.equals(AustraliaConnectIdCheckTrustFrameworkSupported.ConnectIdTrustFramework)) {
			throw error ("Unexpected trusted framework returned in id_token",
				args("expected", AustraliaConnectIdCheckTrustFrameworkSupported.ConnectIdTrustFramework, "actual", idTokenTrustFramework));
		}

		logSuccess("id_token contains the expected trust framework.", args("trust_framework", idTokenTrustFramework));

		return env;
	}
}
