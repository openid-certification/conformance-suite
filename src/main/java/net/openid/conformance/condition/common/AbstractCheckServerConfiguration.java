package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public abstract class AbstractCheckServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		List<String> lookFor = getExpectedListEndpoint();

		for (String key : lookFor) {
			ensureString(env, key);
			ensureUrl(env, key);
		}

		logSuccess("Found required server configuration keys", args("required", lookFor));
		return env;
	}

	protected void ensureString(Environment env, String path) {
		String string = env.getString("server", path);
		if (Strings.isNullOrEmpty(string)) {
			throw error("Couldn't find required component", args("required", path));
		}
	}

	protected void ensureUrl(Environment env, String path) {
		String string = env.getString("server", path);
		try {
			@SuppressWarnings("unused")
			URL url = new URL(string);
		} catch (MalformedURLException e) {
			throw error("Couldn't parse key as URL", e, args("key", path, "url", string));
		}
	}

	protected abstract List<String> getExpectedListEndpoint();
}
