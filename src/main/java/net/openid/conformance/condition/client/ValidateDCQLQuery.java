package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;

public class ValidateDCQLQuery extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject dcql = (JsonObject) env.getElementFromObject("client", "dcql");
		String schemaResource = "json-schemas/oid4vp/dcql_request.json";
		String inputName = "DCQL query";
		return new JsonSchemaValidationInput(inputName, schemaResource, dcql);
	}


	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
