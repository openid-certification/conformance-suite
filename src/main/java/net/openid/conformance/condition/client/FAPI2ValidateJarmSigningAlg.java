package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

public class FAPI2ValidateJarmSigningAlg extends AbstractCondition {
	@Override
	@PreEnvironment(required = "jarm_response")
	public Environment evaluate(Environment env) {
		String alg = env.getString("jarm_response", "header.alg");

		List<String> permitted = Arrays.asList(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported.FAPI2_ALLOWED_ALGS);
		if (permitted.contains(alg)) {
			logSuccess("JARM response was signed with a permitted algorithm",
				args("alg", alg, "permitted", permitted));
			return env;
		}

		throw error("JARM response must be signed with a permitted alg",
			args("alg", alg, "permitted", permitted));
	}
}
