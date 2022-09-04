package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class CreateLogoUri extends AbstractCondition {

	private static final String LOGO_PATH = "/images/openid.png";

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "logo_uri")
	public Environment evaluate(Environment env) {

		URI logoUri;

		try {
			URI baseUri = new URI(env.getString("base_url"));
			logoUri = new URI(
					baseUri.getScheme(),
					null,
					baseUri.getHost(),
					baseUri.getPort(),
					LOGO_PATH,
					null,
					null);
		} catch (URISyntaxException e) {
			throw error("Failed to generate logo URI", e);
		}

		env.putString("logo_uri", logoUri.toString());
		log("Generated logo URI", args("logo_uri", logoUri));

		return env;
	}

}
