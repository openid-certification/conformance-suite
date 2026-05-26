package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
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
		// A present-but-non-string endpoint value is certainly not an https URL; report it as such
		// rather than letting OIDFJSON.getString throw an unexpected exception (the value's type is
		// also flagged by the schema validation).
		JsonElement value = credentialIssuerMetadata.get(uriField);
		if (!OIDFJSON.isString(value)) {
			return false;
		}
		// RFC 3986 §3.1 makes URI scheme names case-insensitive, so HTTPS:// is valid too.
		String uri = OIDFJSON.getString(value);
		return uri.regionMatches(true, 0, "https://", 0, "https://".length());
	}
}
