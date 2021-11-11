package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OverrideCNPJ extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		if(obj == null){
			obj = env.getObject("config");
			obj = obj.getAsJsonObject("resource");
		}
		if(obj == null){
			logFailure("Cannot find resource object.");
			return env;
		}

		JsonObject pixObj = obj.getAsJsonObject("brazilPixPayment");
		String value = "02872369000110";
		String key = "cnpjInitiator";
		JsonObject dataObj = pixObj.getAsJsonObject("data");
		dataObj.addProperty(key, value);

		log("'" + key + "' in payment initiation body set to '"+value+"'",
			args("pix_body", pixObj));

		return env;
	}
}
