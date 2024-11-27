package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedParametersInBindingJwtHeader extends AbstractCondition {
	// https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-14.html#section-4.3
	public static List<String> expectedParams = List.of(
		"typ",
		"alg"
	);

	@Override
	@PreEnvironment(required = { "sdjwt" })
	public Environment evaluate(Environment env) {
		JsonObject parameters = env.getElementFromObject("sdjwt", "binding.header").getAsJsonObject();
		List<String> unknownParameters = new ArrayList<>();

		for (String claim : parameters.keySet()) {
			if (expectedParams.contains(claim)) {
				continue;
			}

			unknownParameters.add(claim);
		}

		if (unknownParameters.isEmpty()) {
			logSuccess("Binding JWT header contains only expected parameters",args("header", parameters.keySet()));
		} else {
			throw error("Unexpected parameters found in binding JWT header", args("header", parameters.keySet(), "unknown", unknownParameters));
		}

		return env;
	}
}
