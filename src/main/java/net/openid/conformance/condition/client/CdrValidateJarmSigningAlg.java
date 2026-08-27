package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CdrValidateJarmSigningAlg extends AbstractCondition {

	private static final List<String> PERMITTED = List.of("PS256", "ES256");

	@Override
	@PreEnvironment(required = "jarm_response")
	public Environment evaluate(Environment env) {
		String alg = env.getString("jarm_response", "header.alg");

		if (PERMITTED.contains(alg)) {
			logSuccess("JARM response was signed with an algorithm CDR permits",
				args("alg", alg, "permitted", PERMITTED));
			return env;
		}

		throw error("JARM response must be signed with PS256 or ES256",
			args("alg", alg, "permitted", PERMITTED));
	}
}
