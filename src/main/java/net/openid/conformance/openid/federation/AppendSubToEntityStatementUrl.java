package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class AppendSubToEntityStatementUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "entity_statement_url", "expected_sub" })
	public Environment evaluate(Environment env) {

		String sub = env.getString("expected_sub");
		String fetchEndpoint = env.getString("entity_statement_url");

		String fetchEndpointUrlWithSubParam = UriComponentsBuilder.fromHttpUrl(fetchEndpoint).queryParam("sub", sub).toUriString();

		env.putString("entity_statement_url", fetchEndpointUrlWithSubParam);

		logSuccess("Appended sub parameter to federation_fetch_endpoint", args("entity_statement_url", fetchEndpointUrlWithSubParam));

		return env;
	}

}
