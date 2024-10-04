package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck extends AbstractCondition {

	private static final Set<String> ENDPOINT_FIELDS = Set.of(
		"jwks_uri",
		"configuration_endpoint",
		"status_endpoint",
		"add_subject_endpoint",
		"remove_subject_endpoint",
		"verification_endpoint"
	);

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();

		Set<String> nonHttpsEndpoints = ENDPOINT_FIELDS.stream()
			.filter(transmitterMetadata::has)
			.filter(endpointField -> !ensureHttpsUrl(transmitterMetadata, endpointField))
			.collect(Collectors.toCollection(LinkedHashSet::new));

		if (!nonHttpsEndpoints.isEmpty()) {
			throw error("Found endpoints not using HTTPS", args("non_https_endpoints", nonHttpsEndpoints));
		}

		logSuccess("All detected endpoints use HTTPS");

		return env;
	}

	private boolean ensureHttpsUrl(JsonObject transmitterMetadata, String uriField) {
		String uri = OIDFJSON.getString(transmitterMetadata.get(uriField));
		return uri.startsWith("https://");
	}
}
