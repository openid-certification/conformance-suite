package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrAddAcrValuesSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonArray acrValuesSupported = new JsonArray();
		acrValuesSupported.add("urn:cds.au:cdr:2");
		acrValuesSupported.add("urn:cds.au:cdr:3");
		server.add("acr_values_supported", acrValuesSupported);

		log("Added the CDR acr values to acr_values_supported in server metadata", args("server", server));

		return env;
	}

}
