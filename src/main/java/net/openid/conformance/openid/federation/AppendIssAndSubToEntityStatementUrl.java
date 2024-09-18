package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

public class AppendIssAndSubToEntityStatementUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "entity_statement_url" })
	public Environment evaluate(Environment env) {

		String iss = env.getString("expected_iss");
		String sub = env.getString("expected_sub");
		String endpoint = env.getString("entity_statement_url");

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
		if (iss != null) {
			builder = builder.queryParam("iss", UriUtils.encode(iss, StandardCharsets.UTF_8));
		}
		if (sub != null) {
			builder = builder.queryParam("sub", UriUtils.encode(sub, StandardCharsets.UTF_8));
		}

		String endpointUrlWithIssParam = builder.build().toUriString();

		env.putString("entity_statement_url", endpointUrlWithIssParam);

		logSuccess("Appended iss and sub parameters to endpoint",
			args("entity_statement_url", endpointUrlWithIssParam, "iss", iss, "sub", sub));

		return env;
	}

}
