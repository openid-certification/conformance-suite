package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GetStaticEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "entity_statement_body", } )
	public Environment evaluate(Environment env) {

		JsonElement entityStatement = env.getElementFromObject("config", "federation.entity_statement");
		if (entityStatement == null) {
			throw error("Couldn't find entityStatement in configuration");
		}

		try {
			JsonObject entityStatementBody = entityStatement.getAsJsonObject();
			logSuccess("Successfully parsed entity statement", entityStatementBody);
			env.putObject("entity_statement_body", entityStatementBody);
			return env;
		} catch (JsonSyntaxException e) {
			throw error(e, args("json", entityStatement));
		}
	}

}
