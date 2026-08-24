package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the discovery document this test will publish contains the provider metadata
 * OpenID Connect Discovery 1.0 section 3 marks as REQUIRED.
 *
 * <p>This is a check on the conformance suite's own behaviour when it acts as an OpenID
 * Provider: a relying party under test is entitled to reject a document that omits any of
 * these, in which case it never reaches the behaviour the test is actually about.
 *
 * <p>Only the presence of the fields is checked, not their contents. In particular the
 * additional requirement that {@code id_token_signing_alg_values_supported} contains RS256 is
 * deliberately not checked here, as a FAPI provider must not claim to support RS256.
 *
 * <p>Not applicable to a provider that publishes RFC 8414 authorization server metadata rather
 * than an OpenID Connect discovery document, nor to a CIBA-only provider (which has no
 * authorization endpoint and hence no response types).
 */
public class EnsureServerConfigurationHasRequiredOidcMetadata extends AbstractCondition {

	private static final String[] REQUIRED_FIELDS = {
		"issuer",
		"authorization_endpoint",
		"token_endpoint",
		"jwks_uri",
		"response_types_supported",
		"subject_types_supported",
		"id_token_signing_alg_values_supported",
	};

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		List<String> missing = new ArrayList<>();
		for (String field : REQUIRED_FIELDS) {
			JsonElement value = server.get(field);
			if (value == null || value.isJsonNull()) {
				missing.add(field);
			}
		}

		if (!missing.isEmpty()) {
			throw error("The discovery document this test publishes is missing metadata OpenID Connect Discovery requires. " +
				"This is a bug in the conformance suite, please report it.",
				args("missing", missing, "server", server));
		}

		logSuccess("The discovery document contains all metadata required by OpenID Connect Discovery", args("server", server));

		return env;
	}
}
