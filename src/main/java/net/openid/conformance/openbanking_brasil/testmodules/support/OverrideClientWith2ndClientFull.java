package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OverrideClientWith2ndClientFull extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "altconfig")
	public Environment evaluate(Environment env) {

		//Create a copy of the config to override client configs with client 2
		JsonObject config = env.getObject("config").deepCopy();
		JsonElement client2 = config.get("client2");
		if(client2 != null){
			config.add("client", client2);
		}else {
			throw error("Second client configuration is not provided");
		}

		JsonElement mtls2 = config.get("mtls2");
		if(mtls2 != null){
			config.add("mtls", mtls2);
		}else {
			throw error("Second client MTLS certificates are not provided");
		}

		JsonObject secondDirectory = (JsonObject) env.getElementFromObject("config", "directory2");
		if (secondDirectory == null || secondDirectory.get("client_id") == null) {
			throw error("Directory configuration for the second client is not provided");
		}

		config.getAsJsonObject("directory").add("client_id", secondDirectory.get("client_id"));
		env.putObject("altconfig", config);
		env.mapKey("config", "altconfig");
		env.mapKey("mutual_tls_authentication", "altmtls");
		return env;
	}
}
