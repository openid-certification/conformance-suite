package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsurePoppyClientAssertionAlgIsAdvertised extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_assertion", "server"})
	public Environment evaluate(Environment env) {
		String alg = env.getString("client_assertion", "header.alg");
		if (alg == null) {
			throw error("The client assertion has no 'alg' header",
				args("header", env.getElementFromObject("client_assertion", "header")));
		}
		JsonElement advertised = env.getElementFromObject("server", "token_endpoint_auth_signing_alg_values_supported");
		if (advertised == null || !advertised.isJsonArray()) {
			throw error("The suite's authorization server metadata has no 'token_endpoint_auth_signing_alg_values_supported' array");
		}
		for (JsonElement e : advertised.getAsJsonArray()) {
			if (OIDFJSON.isString(e) && alg.equals(OIDFJSON.getString(e))) {
				logSuccess("The client assertion is signed with an algorithm the authorization server metadata advertises",
					args("alg", alg, "advertised", advertised));
				return env;
			}
		}
		throw error("The client assertion is signed with an algorithm that the authorization server metadata does not list in 'token_endpoint_auth_signing_alg_values_supported' (RFC 8414 section 2)",
			args("alg", alg, "advertised", advertised));
	}
}
