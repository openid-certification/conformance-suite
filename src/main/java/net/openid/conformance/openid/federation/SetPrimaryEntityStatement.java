package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetPrimaryEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	@PostEnvironment(
		required = { "primary_entity_statement_jwt" },
		strings = { "primary_entity_statement_iss", "primary_entity_statement_sub" }
	)
	public Environment evaluate(Environment env) {

		env.putObject("primary_entity_statement_endpoint_response", env.getObject("federation_http_response"));
		env.putObject("primary_entity_statement_jwt", env.getObject("federation_response_jwt"));

		env.putString("primary_entity_statement_iss", OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "claims.iss")));
		env.putString("primary_entity_statement_sub", OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "claims.sub")));

		logSuccess("Primary entity statement set");

		return env;
	}

}
