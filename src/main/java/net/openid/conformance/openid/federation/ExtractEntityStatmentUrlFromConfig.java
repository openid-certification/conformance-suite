package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


public class ExtractEntityStatmentUrlFromConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(strings = "entity_statement_url")
	public Environment evaluate(Environment env) {

		String entityStatementUrl = env.getString("config", "server.entityStatementUrl");
		env.putString("entity_statement_url", entityStatementUrl);

		logSuccess("Found entity statement URL in config", args("entity_statement_url", entityStatementUrl));

		return env;
	}

}

