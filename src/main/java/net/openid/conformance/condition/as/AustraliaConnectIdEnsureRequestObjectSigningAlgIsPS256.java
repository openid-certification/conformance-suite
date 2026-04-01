package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String alg = env.getString("authorization_request_object", "header.alg");
		if (Strings.isNullOrEmpty(alg)) {
			throw error("Request object does not contain an alg header");
		}
		if (!"PS256".equals(alg)) {
			throw error("Request object must be signed with PS256", args("alg", alg));
		}
		logSuccess("Request object is signed with PS256");
		return env;
	}
}
