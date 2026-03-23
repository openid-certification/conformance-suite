package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;

public class CheckForUnexpectedParametersInDcqlQuery extends AbstractCheckForUnexpectedSchemaProperties {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		return new JsonSchemaValidationInput("DCQL query",
			"json-schemas/oid4vp/dcql_request.json", dcql);
	}

	@Override
	@PreEnvironment(required = ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
