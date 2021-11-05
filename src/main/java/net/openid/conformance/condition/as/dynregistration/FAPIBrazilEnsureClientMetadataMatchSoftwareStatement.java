package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilEnsureClientMetadataMatchSoftwareStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "software_statement"})
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("dynamic_registration_request");
		JsonObject statement = env.getObject("software_statement");
		//TODO compare other fields?
		compareElements(request, statement, "client_name", "software_client_name");
		compareElements(request, statement, "tos_uri", "software_tos_uri");
		compareElements(request, statement, "policy_uri", "software_policy_uri");
		compareElements(request, statement, "logo_uri", "software_logo_uri");
		logSuccess("Client metadata matches software statement");
		return env;
	}

	protected void compareElements(JsonObject request, JsonObject statement, String requestElementName, String statementElementName){
		if(statement.has(statementElementName) && request.has(requestElementName)) {
			if(!request.get(requestElementName).equals(statement.get(statementElementName))) {
				throw error(requestElementName + " does not match " + statementElementName, args(requestElementName, request.get(requestElementName),
					statementElementName, statement.get(statementElementName)));
			}
		}
	}

}
