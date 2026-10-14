package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Section 9.1.2: capabilities is a "list of registered IANA URNs referencing PDP specific
 * capabilities". Section 12.3 creates the registry for them and Section 12.4 registers the
 * "authzen" sub-namespace of the IETF parameters URN namespace (RFC 3553), so a registered
 * capability is a URN beginning urn:ietf:params:authzen:. The registry has no entries yet, so
 * whether a value is registered cannot be checked; this flags values outside that
 * sub-namespace. Intended to be called at WARNING. Whether capabilities is an array of strings
 * is checked by {@link EnsureMetadataCapabilitiesValid}.
 */
public class WarnAuthzenCapabilitiesNotAuthzenUrns extends AbstractCondition {

	private static final String AUTHZEN_URN_PREFIX = "urn:ietf:params:authzen:";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		JsonElement capabilities = server.get("capabilities");
		if (capabilities == null || !capabilities.isJsonArray()) {
			log("Metadata has no capabilities array to check");
			return env;
		}
		JsonArray values = capabilities.getAsJsonArray();
		List<String> outside = new ArrayList<>();
		for (JsonElement value : values) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
				continue;
			}
			String capability = OIDFJSON.getString(value);
			// The "urn" scheme and the namespace identifiers are case-insensitive (RFC 8141 3.1).
			String lower = capability.toLowerCase(Locale.ROOT);
			if (!lower.startsWith(AUTHZEN_URN_PREFIX) || lower.length() == AUTHZEN_URN_PREFIX.length()) {
				outside.add(capability);
			}
		}
		if (!outside.isEmpty()) {
			throw error("Metadata capabilities contains values that are not URNs in the urn:ietf:params:authzen: sub-namespace of the AuthZEN Policy Decision Point Capabilities registry",
				args("not_authzen_urns", outside, "capabilities", values));
		}
		logSuccess("Every metadata capability is a URN in the urn:ietf:params:authzen: sub-namespace; the registry has no entries yet, so registration is not checked",
			args("capabilities", values));
		return env;
	}
}
