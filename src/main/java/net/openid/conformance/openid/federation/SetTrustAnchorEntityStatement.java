package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetTrustAnchorEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "entity_statement_endpoint_response", "entity_statement_body", "entity_statement_header" } )
	@PostEnvironment(required = {
		"trust_anchor_entity_statement_endpoint_response",
		"trust_anchor_entity_statement_body",
		"trust_anchor_entity_statement_header"
	}, strings = {
		"trust_anchor_entity_statement_iss",
		"trust_anchor_entity_statement_sub"
	})
	public Environment evaluate(Environment env) {

		env.putObject("trust_anchor_entity_statement_endpoint_response", env.getObject("entity_statement_endpoint_response"));
		env.putObject("trust_anchor_entity_statement_body", env.getObject("entity_statement_body"));
		env.putObject("trust_anchor_entity_statement_header", env.getObject("entity_statement_header"));

		env.putString("trust_anchor_entity_statement_iss", OIDFJSON.getString(env.getElementFromObject("entity_statement_body", "iss")));
		env.putString("trust_anchor_entity_statement_sub", OIDFJSON.getString(env.getElementFromObject("entity_statement_body", "sub")));

		logSuccess("Trust anchor entity statement set");

		return env;
	}

}
