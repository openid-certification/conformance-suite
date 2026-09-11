package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * SSF 1.0 7.1: the {@code issuer} of the Transmitter Configuration Metadata is a "URL using
 * the https scheme with no query or fragment component that the Transmitter asserts as its
 * Issuer Identifier".
 */
public class OIDSSFEnsureTransmitterMetadataIssuerIsValidUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement issuerEl = env.getElementFromObject("ssf", "transmitter_metadata.issuer");
		if (issuerEl == null) {
			log("The transmitter metadata has no 'issuer'; that is graded by the required-fields check",
				args("transmitter_metadata", env.getElementFromObject("ssf", "transmitter_metadata")));
			return env;
		}
		if (!issuerEl.isJsonPrimitive() || !issuerEl.getAsJsonPrimitive().isString()) {
			throw error("The transmitter metadata 'issuer' must be a JSON string", args("issuer", issuerEl));
		}

		String issuer = OIDFJSON.getString(issuerEl);
		if (issuer.isBlank()) {
			throw error("The transmitter metadata 'issuer' must not be empty", args("issuer", issuer));
		}

		URI issuerUri;
		try {
			issuerUri = new URI(issuer);
		} catch (URISyntaxException e) {
			throw error("The transmitter metadata 'issuer' is not a valid URL", args("issuer", issuer, "error", e.getMessage()));
		}

		List<String> problems = new ArrayList<>();
		if (!"https".equalsIgnoreCase(issuerUri.getScheme())) {
			problems.add("the scheme is not https");
		}
		if (issuerUri.getHost() == null) {
			problems.add("the URL has no host");
		}
		if (issuerUri.getRawQuery() != null) {
			problems.add("the URL contains a query component");
		}
		if (issuerUri.getRawFragment() != null) {
			problems.add("the URL contains a fragment component");
		}

		if (!problems.isEmpty()) {
			throw error("The transmitter metadata 'issuer' must be a URL using the https scheme with no query or fragment component",
				args("issuer", issuer, "problems", problems));
		}

		logSuccess("The transmitter metadata 'issuer' is an https URL without query or fragment", args("issuer", issuer));

		return env;
	}
}
