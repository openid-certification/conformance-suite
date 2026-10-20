package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the endpoints of the PDP metadata after the signed_metadata values have been applied.
 * An endpoint the signed values do not carry is the published one, which
 * {@link CheckPublishedPDPMetadataEndpoints} has already checked, so only the endpoints the signed
 * values carry are checked here, and a REQUIRED endpoint is still required to be present.
 */
public class CheckSignedPDPServerConfiguration extends CheckPDPServerConfiguration {

	@Override
	@PreEnvironment(required = {"server", "pdp_signed_metadata_values"})
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		JsonObject signedValues = env.getObject("pdp_signed_metadata_values");
		List<String> published = new ArrayList<>();

		for (String key : getRequiredEndpoints()) {
			if (signedValues.has(key) || !server.has(key)) {
				env = validate(env, key);
			} else {
				published.add(key);
			}
		}

		for (String endpoint : getOptionalEndpoints()) {
			if (signedValues.has(endpoint)) {
				env = validate(env, endpoint);
			} else if (server.has(endpoint)) {
				published.add(endpoint);
			}
		}

		if (!published.isEmpty()) {
			log("Endpoints signed_metadata does not override are the published ones, already checked",
				args("published_endpoints", published));
		}
		return env;
	}
}
