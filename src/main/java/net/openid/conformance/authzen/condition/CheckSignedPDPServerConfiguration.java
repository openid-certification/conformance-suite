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
 * values carry are checked here. A REQUIRED endpoint missing from both has already failed
 * {@link EnsurePublishedMetadataHasAccessEvaluationEndpoint} or
 * {@link EnsurePolicyDecisionPointMatchesIssuer}, which run on the published document, so it is
 * not reported a second time.
 */
public class CheckSignedPDPServerConfiguration extends CheckPDPServerConfiguration {

	@Override
	@PreEnvironment(required = {"server", "pdp_signed_metadata_values"})
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		JsonObject signedValues = env.getObject("pdp_signed_metadata_values");
		List<String> published = new ArrayList<>();
		List<String> missing = new ArrayList<>();

		for (String key : getRequiredEndpoints()) {
			if (signedValues.has(key)) {
				env = validate(env, key);
			} else if (server.has(key)) {
				published.add(key);
			} else {
				missing.add(key);
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
		if (!missing.isEmpty()) {
			log("Required endpoints in neither the published document nor signed_metadata, already reported "
				+ "as missing from the published document", args("missing_endpoints", missing));
		}
		return env;
	}
}
