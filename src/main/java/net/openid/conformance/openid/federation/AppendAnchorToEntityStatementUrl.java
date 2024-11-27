package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class AppendAnchorToEntityStatementUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config", strings = { "federation_endpoint_url" })
	public Environment evaluate(Environment env) {

		String anchor = env.getString("config", "federation.trust_anchor");
		String endpoint = env.getString("federation_endpoint_url");

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
		if (anchor != null) {
			builder = builder.queryParam("anchor", anchor);
		}

		String endpointUrlWithAnchorParam = builder.build().toUriString();

		env.putString("federation_endpoint_url", endpointUrlWithAnchorParam);

		logSuccess("Appended anchor parameter to endpoint", args("federation_endpoint_url", endpointUrlWithAnchorParam, "anchor", anchor));

		return env;
	}

}