package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilAddOBBrazilAcrValuesSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");
		{
			JsonArray acrValuesSupported = new JsonArray();
			acrValuesSupported.add("urn:brasil:openbanking:loa2");
			acrValuesSupported.add("urn:brasil:openbanking:loa3");
			server.add("acr_values_supported", acrValuesSupported);
		}

		log("Added open banking Brazil specific acr_values_supported values", args("server", server));
		return env;
	}

}
