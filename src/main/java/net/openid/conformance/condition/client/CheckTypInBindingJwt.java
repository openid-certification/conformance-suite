package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckTypInBindingJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {
		String typ = env.getString("sdjwt", "binding.header.typ");
		final String expected = "kb+jwt";
		if (typ == null) {
			throw error("'typ' claim missing");
		}

		if (!typ.equals(expected)) {
			throw error("typ in binding JWT header is incorrect",
				args("actual", typ, "expected", expected));
		}

		logSuccess("typ in the binding jwt is " + expected);

		return env;
	}
}
