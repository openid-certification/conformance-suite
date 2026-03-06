package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class ValidateCredentialJWTIssIsHttpsUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {

		String iss = env.getString("sdjwt", "credential.claims.iss");

		if (iss == null) {
			throw error("required claim 'iss' is not present");
		}

		try {
			URI uri = new URI(iss);
			if (uri.getScheme() == null) {
				throw error("'iss' is not a valid URI - missing scheme", args("iss", iss));
			}
			if (!"https".equalsIgnoreCase(uri.getScheme())) {
				throw error("'iss' must use the https scheme", args("iss", iss, "scheme", uri.getScheme()));
			}
			if (uri.getHost() == null) {
				throw error("'iss' is not a valid HTTPS URI - missing host", args("iss", iss));
			}
		} catch (URISyntaxException e) {
			throw error("'iss' is not a valid URI", e, args("iss", iss));
		}

		logSuccess("'iss' is a valid HTTPS URI", args("iss", iss));

		return env;
	}

}
