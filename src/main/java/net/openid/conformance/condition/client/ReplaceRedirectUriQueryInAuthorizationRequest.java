package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.DefaultUriBuilderFactory;

public class ReplaceRedirectUriQueryInAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_uri")
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("authorization_endpoint_request");

		if (!request.has("redirect_uri")) {
			throw error("redirect_uri was not found in authorization_endpoint_request");
		}

		String redirectUri = OIDFJSON.getString(request.get("redirect_uri"));

		String redirectUriWithQuery =
				new DefaultUriBuilderFactory()
						.uriString(redirectUri)
						.replaceQuery(null)
						.queryParam("foo", "bar")
						.build()
						.toString();

		request.remove("redirect_uri");
		request.addProperty("redirect_uri", redirectUriWithQuery);

		log("Updated redirect_uri in authorization endpoint request", args("redirect_uri", redirectUriWithQuery));

		return env;
	}

}
