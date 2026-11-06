package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public class FetchPoppyProtectedResourceMetadata extends AbstractPoppyFetch {

	static URI url(String domain) {
		return URI.create("https://" + domain + "/.well-known/oauth-protected-resource");
	}

	@Override
	@PreEnvironment(strings = "poppy_domain")
	@PostEnvironment(required = "poppy_protected_resource_metadata")
	public Environment evaluate(Environment env) {
		URI url = url(env.getString("poppy_domain"));
		ResponseEntity<String> response = get(env, url);
		if (response.getStatusCode().value() == 404) {
			throw error("The Company publishes no protected resource metadata, so it has no issuer. This test exercises Direct Sign-In and needs the Company to offer sign-in to Personal Agents.",
				args("url", url.toString()));
		}
		JsonObject metadata = requireJsonObject(response, url, "protected resource metadata");
		env.putObject("poppy_protected_resource_metadata", metadata);
		logSuccess("Fetched protected resource metadata", args("url", url.toString(), "metadata", metadata));
		return env;
	}
}
