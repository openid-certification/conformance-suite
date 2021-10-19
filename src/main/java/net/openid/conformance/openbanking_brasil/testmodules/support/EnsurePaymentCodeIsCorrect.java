package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

import java.util.Base64;

public class EnsurePaymentCodeIsCorrect extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String jwt = env.getString("resource_endpoint_response");
		String body = jwt.split("\\.")[1];
		body = new String(Base64.getUrlDecoder().decode(body));
		JsonObject json = stringToJson(body);

		JsonArray errors = json.getAsJsonArray("errors");
		JsonObject error = errors.get(0).getAsJsonObject();
		String code = OIDFJSON.getString(error.get("code"));

		if(code.equalsIgnoreCase("PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO") || code.equalsIgnoreCase("VALOR_INCOMPATIVEL")){
			logSuccess("Code is correct - payment divergent from consent");
		} else {
			logFailure("Code is incorrect - needs to be PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO or VALOR_INCOMPATIVEL");
		}

		return env;
	}

	private JsonObject stringToJson(String json){
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		return gson.fromJson(json, JsonObject.class);
	}
}
