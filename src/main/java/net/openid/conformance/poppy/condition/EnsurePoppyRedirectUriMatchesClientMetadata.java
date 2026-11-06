package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class EnsurePoppyRedirectUriMatchesClientMetadata extends AbstractCondition {

	private static final String REQUEST = "effective_authorization_endpoint_request";

	@Override
	@PreEnvironment(required = {REQUEST, "client"})
	@PostEnvironment(strings = "authorization_endpoint_request_redirect_uri")
	public Environment evaluate(Environment env) {
		String redirectUri = env.getString(REQUEST, "redirect_uri");
		if (redirectUri == null || redirectUri.isEmpty()) {
			throw error("The authorization request has no 'redirect_uri'. Direct Sign-In requires it (section 4.2).");
		}
		JsonElement registered = env.getElementFromObject("client", "redirect_uris");
		if (registered == null || !registered.isJsonArray()) {
			throw error("The client metadata document has no 'redirect_uris' array to compare the redirect_uri against",
				args("redirect_uri", redirectUri));
		}

		JsonArray uris = registered.getAsJsonArray();
		for (JsonElement e : uris) {
			if (!OIDFJSON.isString(e)) {
				continue;
			}
			String entry = OIDFJSON.getString(e);
			if (entry.equals(redirectUri) || matchesLoopbackIgnoringPort(entry, redirectUri)) {
				env.putString("authorization_endpoint_request_redirect_uri", redirectUri);
				logSuccess("The redirect_uri matches an entry in the client metadata document",
					args("redirect_uri", redirectUri, "matched_entry", entry));
				return env;
			}
		}
		throw error("The redirect_uri does not match any entry in the client metadata document's 'redirect_uris'. Section 4.2 requires an exact match, except for the port of a loopback redirect URI (RFC 8252 section 7.3).",
			args("redirect_uri", redirectUri, "redirect_uris", uris));
	}

	private boolean matchesLoopbackIgnoringPort(String registered, String requested) {
		try {
			URI reg = new URI(registered);
			URI req = new URI(requested);
			if (!"http".equals(reg.getScheme()) || !isLoopbackLiteral(reg.getHost())) {
				return false;
			}
			return "http".equals(req.getScheme())
				&& reg.getHost().equals(req.getHost())
				&& Objects.equals(reg.getRawPath(), req.getRawPath())
				&& Objects.equals(reg.getRawQuery(), req.getRawQuery())
				&& reg.getRawFragment() == null && req.getRawFragment() == null
				&& reg.getRawUserInfo() == null && req.getRawUserInfo() == null;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private boolean isLoopbackLiteral(String host) {
		return "127.0.0.1".equals(host) || "[::1]".equals(host);
	}
}
