package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class AppendIssToFederationFetchEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "federation_fetch_endpoint", "entity_statement_iss" })
	@PostEnvironment(strings = "entity_statement_url")
	public Environment evaluate(Environment env) {

		String iss = env.getString("entity_statement_iss");
		String fetchEndpoint = env.getString("federation_fetch_endpoint");

		String fetchEndpointUrlWithIssParam = UriComponentsBuilder.fromHttpUrl(fetchEndpoint).queryParam("iss", iss).toUriString();

		env.putString("entity_statement_url", fetchEndpointUrlWithIssParam);

		logSuccess("Appended iss parameter to federation_fetch_endpoint", args("federation_fetch_endpoint", fetchEndpointUrlWithIssParam));

		return env;
	}

}
