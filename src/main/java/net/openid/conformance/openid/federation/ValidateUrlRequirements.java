package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class ValidateUrlRequirements extends AbstractCondition {

	protected Environment validateUrlRequirements(JsonElement urlStringElement, String fieldName, String label, Environment env) {
		if (urlStringElement == null || !urlStringElement.getAsJsonPrimitive().isString()) {
			throw error("%s is missing or null".formatted(label), args(fieldName, urlStringElement));
		}

		String urlString = OIDFJSON.getString(urlStringElement);
		try {
			URL url = new URL(urlString);
			if (!"https".equals(url.getProtocol())) {
				throw error("%s must use the https scheme".formatted(label), args(fieldName, urlString));
			}

			if (url.getHost().isEmpty()) {
				throw error("%s must have a host component".formatted(label), args(fieldName, urlString));
			}

			if (url.getQuery() != null) {
				throw error("%s must not contain query parameters".formatted(label), args(fieldName, urlString));
			}

			if (url.getRef() != null) {
				throw error("%s must not contain a fragment component".formatted(label), args(fieldName, urlString));
			}

			logSuccess("%s is a valid URL with the required components".formatted(label), args(fieldName, urlString));
			return env;
		} catch (MalformedURLException e) {
			throw error("%s is not a valid URL".formatted(label), args(fieldName, urlString));
		}
	}
}
