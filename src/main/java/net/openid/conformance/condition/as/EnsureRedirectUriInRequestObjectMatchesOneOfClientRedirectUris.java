package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureRedirectUriInRequestObjectMatchesOneOfClientRedirectUris extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "authorization_request_object" })
	@PostEnvironment(strings = {"authorization_endpoint_request_redirect_uri"})
	public Environment evaluate(Environment env) {
		// get the client ID from the configuration
		JsonArray redirectUris = env.getElementFromObject("client", "redirect_uris").getAsJsonArray();
		String actual = env.getString("authorization_request_object", "claims.redirect_uri");

		for(JsonElement redirUri : redirectUris) {
			String uriString = OIDFJSON.getString(redirUri);
			if(actual.equals(uriString)) {
				env.putString("authorization_endpoint_request_redirect_uri", actual);
				logSuccess("Redirect URI matched one of client redirect_uris",
					args("actual", Strings.nullToEmpty(actual)));
				return env;
			}
		}
		throw error("Redirect URI is not one of the registered ones for the client",
			args("expected", redirectUris, "actual", Strings.nullToEmpty(actual)));

	}

}
