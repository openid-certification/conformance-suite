package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class VCIInjectOpenIdCredentialAsSupportedAuthorizationRequestTypes extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		if (env.getElementFromObject("config", "resource") == null) {
			env.putObject("config", "resource", new JsonObject());
		}

		JsonObject resourceObject = env.getElementFromObject("config", "resource").getAsJsonObject();
		// inject openid_credential types to authorization_details_types_supported config
		resourceObject.add("authorization_details_types_supported", OIDFJSON.convertListToJsonArray(List.of("openid_credential")));

		log("Added openid_credential to authorization_details_types_supported");

		return env;
	}
}
