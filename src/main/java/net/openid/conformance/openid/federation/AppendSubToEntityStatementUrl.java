package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class AppendSubToEntityStatementUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "entity_statement_url" })
	public Environment evaluate(Environment env) {

		String sub = env.getString("expected_sub");
		String endpoint = env.getString("entity_statement_url");

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
		if (sub != null) {
			builder = builder.queryParam("sub", sub);
		}

		String endpointUrlWithSubParam = builder.build().toUriString();

		env.putString("entity_statement_url", endpointUrlWithSubParam);

		logSuccess("Appended sub parameter to endpoint", args("entity_statement_url", endpointUrlWithSubParam, "sub", sub));

		return env;
	}

}
