package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

public class AppendSubToEntityStatementUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "entity_statement_url", "expected_sub" })
	public Environment evaluate(Environment env) {

		String sub = env.getString("expected_sub");
		String endpoint = env.getString("entity_statement_url");

		String endpointUrlWithSubParam = UriComponentsBuilder.fromHttpUrl(endpoint)
			.queryParam("sub", UriUtils.encode(sub, StandardCharsets.UTF_8.toString()))
			.toUriString();

		env.putString("entity_statement_url", endpointUrlWithSubParam);

		logSuccess("Appended sub parameter to endpoint", args("entity_statement_url", endpointUrlWithSubParam));

		return env;
	}

}
