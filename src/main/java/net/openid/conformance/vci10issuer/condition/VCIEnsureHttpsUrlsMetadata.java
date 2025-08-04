package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VCIEnsureHttpsUrlsMetadata extends AbstractCondition {

	private static final Set<String> ENDPOINT_FIELDS = Set.of(
		"credential_issuer",
		"credential_endpoint",
		"nonce_endpoint",
		"deferred_credential_endpoint",
		"notification_endpoint"
	);

	@Override
	@PreEnvironment(required = {"vci"})
	public Environment evaluate(Environment env) {

		JsonObject credentialIssuerMetadata = env.getElementFromObject("vci","credential_issuer_metadata").getAsJsonObject();

		Set<String> nonHttpsEndpoints = ENDPOINT_FIELDS.stream()
			.filter(Predicate.not(Objects::isNull))
			.filter(credentialIssuerMetadata::has)
			.filter(endpointField -> !ensureHttpsUrl(credentialIssuerMetadata, endpointField))
			.collect(Collectors.toCollection(LinkedHashSet::new));

		if (!nonHttpsEndpoints.isEmpty()) {
			throw error("Found endpoints not using HTTPS", args("non_https_endpoints", nonHttpsEndpoints));
		}

		logSuccess("All detected endpoints use HTTPS");

		return env;
	}

	private boolean ensureHttpsUrl(JsonObject credentialIssuerMetadata, String uriField) {
		String uri = OIDFJSON.getString(credentialIssuerMetadata.get(uriField));
		return uri.startsWith("https://");
	}
}
