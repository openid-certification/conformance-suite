package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

import java.util.Base64;

public class Ensure422ResponseCodeWasPARAMETRO_INVALIDO extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String jwt = env.getString("resource_endpoint_response");
		String body = jwt.split("\\.")[1];
		body = new String(Base64.getUrlDecoder().decode(body));
		JsonObject json = stringToJson(body);

		JsonArray errors = json.getAsJsonArray("errors");
		JsonObject error = errors.get(0).getAsJsonObject();
		String status = OIDFJSON.getString(error.get("code"));

		if (status.equalsIgnoreCase("PARAMETRO_INVALIDO")) {
			logSuccess("Error code is PARAMETRO_INVALIDO  as expected");
		} else {
			throw error ("Incorrect error code "+ status +". PARAMETRO_INVALIDO");
		}

		return env;
	}

	private JsonObject stringToJson(String json){
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		return gson.fromJson(json, JsonObject.class);
	}
}
