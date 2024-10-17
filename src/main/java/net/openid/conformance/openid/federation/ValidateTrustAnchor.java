package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public class ValidateTrustAnchor extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "config" } )
	public Environment evaluate(Environment env) {

		JsonElement trustAnchorElement = env.getElementFromObject("config", "federation.trust_anchor");
		if (trustAnchorElement == null || !trustAnchorElement.getAsJsonPrimitive().isString()) {
			throw error("Trust anchor is missing or null", args("trust_anchor", trustAnchorElement));
		}

		String entityIdentifier = OIDFJSON.getString(trustAnchorElement);
		try {
			URL url = new URL(entityIdentifier);

			if (!"https".equals(url.getProtocol())) {
				throw error("Trust anchor must use the https scheme", args("trust_anchor", entityIdentifier));
			}

			if (url.getHost().isEmpty()) {
				throw error("Trust anchor must have a host component", args("trust_anchor", entityIdentifier));
			}

			if (url.getQuery() != null) {
				throw error("Trust anchor must not contain query parameters", args("trust_anchor", entityIdentifier));
			}

			if (url.getRef() != null) {
				throw error("Trust anchor must not contain a fragment component", args("trust_anchor", entityIdentifier));
			}

			logSuccess("Trust anchor is a valid URL with the required components", args("trust_anchor", entityIdentifier));
			return env;
		} catch (MalformedURLException e) {
			throw error("Trust anchor is not a valid URL", args("trust_anchor", entityIdentifier));
		}
	}
}
