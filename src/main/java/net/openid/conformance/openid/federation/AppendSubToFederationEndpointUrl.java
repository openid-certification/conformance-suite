package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class AppendSubToFederationEndpointUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "federation_endpoint_url" })
	public Environment evaluate(Environment env) {

		String sub = env.getString("expected_sub");
		String endpoint = env.getString("federation_endpoint_url");

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
		if (sub != null) {
			builder = builder.queryParam("sub", sub);
		}

		String endpointUrlWithSubParam = builder.build().toUriString();

		env.putString("federation_endpoint_url", endpointUrlWithSubParam);

		logSuccess("Appended sub parameter to endpoint", args("federation_endpoint_url", endpointUrlWithSubParam, "sub", sub));

		return env;
	}

}
