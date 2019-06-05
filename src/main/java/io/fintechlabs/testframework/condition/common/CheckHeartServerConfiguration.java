package io.fintechlabs.testframework.condition.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckHeartServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment in) {

		// first make sure we've got a "server" object at all
		if (!in.containsObject("server")) {
			throw error("Couldn't find a server configuration at all");
		}

		List<String> lookFor = ImmutableList.of("authorization_endpoint", "token_endpoint", "issuer", "introspection_endpoint", "revocation_endpoint", "jwks_uri");

		for (String key : lookFor) {
			ensureString(in, key);

			ensureUrl(in, key);
		}

		logSuccess("Found required server configuration keys", args("required", lookFor));

		return in;
	}

	private void ensureString(Environment in, String path) {
		String string = in.getString("server", path);
		if (Strings.isNullOrEmpty(string)) {
			throw error("Couldn't find required component", args("required", path));
		}
	}

	private void ensureUrl(Environment in, String path) {
		String string = in.getString("server", path);

		try {
			URL url = new URL(string);
		} catch (MalformedURLException e) {
			throw error("Couldn't parse key as URL", e, args("required", path, "url", string));
		}

	}

}
