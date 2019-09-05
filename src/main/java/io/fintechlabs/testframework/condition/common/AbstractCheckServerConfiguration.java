package io.fintechlabs.testframework.condition.common;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public abstract class AbstractCheckServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment in) {

		List<String> lookFor = getExpectedListEndpoint();

		for (String key : lookFor) {
			ensureString(in, key);
			ensureUrl(in, key);
		}

		logSuccess("Found required server configuration keys", args("required", lookFor));
		return in;
	}

	protected void ensureString(Environment in, String path) {
		String string = in.getString("server", path);
		if (Strings.isNullOrEmpty(string)) {
			throw error("Couldn't find required component", args("required", path));
		}
	}

	protected void ensureUrl(Environment in, String path) {
		String string = in.getString("server", path);
		try {
			URL url = new URL(string);
		} catch (MalformedURLException e) {
			throw error("Couldn't parse key as URL", e, args("required", path, "url", string));
		}
	}

	protected abstract List<String> getExpectedListEndpoint();
}
