package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public class ValidateEntityIdentifier extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "config" } )
	public Environment evaluate(Environment env) {

		JsonElement entityIdentifierElement = env.getElementFromObject("config", "federation.entity_identifier");
		if (entityIdentifierElement == null || !entityIdentifierElement.getAsJsonPrimitive().isString()) {
			throw error("Entity identifier is missing or null", args("entity_identifier", entityIdentifierElement));
		}

		String entityIdentifier = OIDFJSON.getString(entityIdentifierElement);
		try {
			URL url = new URL(entityIdentifier);

			if (!"https".equals(url.getProtocol())) {
				throw error("Entity identifier must use the https scheme", args("entity_identifier", entityIdentifier));
			}

			if (url.getHost().isEmpty()) {
				throw error("Entity identifier must have a host component", args("entity_identifier", entityIdentifier));
			}

			if (url.getQuery() != null) {
				throw error("Entity identifier must not contain query parameters", args("entity_identifier", entityIdentifier));
			}

			if (url.getRef() != null) {
				throw error("Entity identifier must not contain a fragment component", args("entity_identifier", entityIdentifier));
			}

			logSuccess("Entity identifier is a valid URL with the required components", args("entity_identifier", entityIdentifier));
			return env;
		} catch (MalformedURLException e) {
			throw error("Entity identifier is not a valid URL", args("entity_identifier", entityIdentifier));
		}
	}
}
