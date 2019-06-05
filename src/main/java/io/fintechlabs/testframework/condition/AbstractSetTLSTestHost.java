package io.fintechlabs.testframework.condition;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractSetTLSTestHost extends AbstractCondition {

	private static final int HTTPS_DEFAULT_PORT = 443;

	protected Environment setTLSTestHost(Environment env, String host, int port) {

		JsonObject o = new JsonObject();
		o.addProperty("testHost", host);
		o.addProperty("testPort", port);

		env.removeObject("tls");
		env.putObject("tls", o);

		logSuccess("Configured TLS test host", o);

		return env;
	}

	protected Environment setTLSTestHost(Environment env, String url) {

		UriComponents components = UriComponentsBuilder.fromUriString(url).build();

		String host = components.getHost();
		int port = components.getPort();

		if (port < 0) {
			port = HTTPS_DEFAULT_PORT;
		}

		return setTLSTestHost(env, host, port);
	}

}
