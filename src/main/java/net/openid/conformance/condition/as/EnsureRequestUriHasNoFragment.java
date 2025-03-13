package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class EnsureRequestUriHasNoFragment extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params"})
	public Environment evaluate(Environment env) {
		String requestUri = env.getString("authorization_endpoint_http_request_params", "request_uri");

		URI uri = null;
		try {
			uri = new URI(requestUri);
		} catch (URISyntaxException e) {
			throw error("Invalid request_uri", e);
		}
		String fragment = uri.getFragment();
		if (fragment != null) {
			throw error("request_uri must not contain a fragment. The fragment is not used for anything so adds no useful value, but does create interoperability issues as some clients fail to strip the fragment before doing a HTTP GET.",
				args("request_uri", requestUri, "fragment", fragment));
		}
		logSuccess("request_uri does not contain a fragment", args("request_uri", requestUri));
		return env;
	}
}
