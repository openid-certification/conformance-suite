package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Flags members of the client metadata document that are neither RFC 7591 section 2 client metadata
 * nor the CIMD {@code client_id}. Human-readable names may carry a language tag
 * ({@code client_name#de}, RFC 7591 section 2.2).
 */
public class CheckForUnexpectedFieldsInPoppyClientMetadataDocument extends AbstractCheckForUnexpectedPoppyFields {

	private static final Set<String> KNOWN = Set.of("client_id", "redirect_uris", "token_endpoint_auth_method",
		"grant_types", "response_types", "client_name", "client_uri", "logo_uri", "scope", "contacts", "tos_uri",
		"policy_uri", "jwks_uri", "jwks", "software_id", "software_version", "software_statement");
	private static final Set<String> LANGUAGE_TAGGED = Set.of("client_name", "client_uri", "logo_uri", "tos_uri", "policy_uri");

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject doc = env.getObject("client");
		List<String> unexpected = new ArrayList<>();
		for (String key : doc.keySet()) {
			int hash = key.indexOf('#');
			boolean taggedName = hash > 0 && hash < key.length() - 1 && LANGUAGE_TAGGED.contains(key.substring(0, hash));
			if (!KNOWN.contains(key) && !taggedName) {
				unexpected.add(key);
			}
		}
		if (!unexpected.isEmpty()) {
			throw error("The client metadata document contains fields that are neither RFC 7591 client metadata nor defined by Poppy 0.4",
				args("unexpected_fields", unexpected));
		}
		logSuccess("The client metadata document contains only known client metadata fields");
		return env;
	}
}
