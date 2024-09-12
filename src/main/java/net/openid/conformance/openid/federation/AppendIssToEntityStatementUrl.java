package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class AppendIssToEntityStatementUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "entity_statement_url", "expected_iss" })
	public Environment evaluate(Environment env) {

		String iss = env.getString("expected_iss");
		String endpoint = env.getString("entity_statement_url");

		String endpointUrlWithIssParam = UriComponentsBuilder.fromHttpUrl(endpoint)
			.queryParam("iss", iss)
			.encode()
			.toUriString();

		env.putString("entity_statement_url", endpointUrlWithIssParam);

		logSuccess("Appended iss parameter to endpoint", args("entity_statement_url", endpointUrlWithIssParam));

		return env;
	}

}
