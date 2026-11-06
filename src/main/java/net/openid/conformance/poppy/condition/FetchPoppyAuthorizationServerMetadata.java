package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.OAuthUriUtil;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public class FetchPoppyAuthorizationServerMetadata extends AbstractPoppyFetch {

	static URI url(String issuer) {
		return URI.create(OAuthUriUtil.generateWellKnownUrlForPath(issuer, "oauth-authorization-server"));
	}

	@Override
	@PreEnvironment(strings = "poppy_issuer")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		URI url = url(env.getString("poppy_issuer"));
		ResponseEntity<String> response = get(env, url);
		JsonObject metadata = requireJsonObject(response, url, "authorization server metadata");
		env.putObject("server", metadata);
		logSuccess("Fetched authorization server metadata", args("url", url.toString(), "server", metadata));
		return env;
	}
}
