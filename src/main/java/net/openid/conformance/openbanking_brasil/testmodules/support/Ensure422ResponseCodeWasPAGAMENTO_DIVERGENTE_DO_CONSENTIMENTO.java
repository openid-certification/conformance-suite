package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class Ensure422ResponseCodeWasPAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resourceEndpointResponse = env.getObject("resource_endpoint_response");
		JsonArray errors = resourceEndpointResponse.getAsJsonArray("errors");

		String status = OIDFJSON.getString(errors.get(0).getAsJsonObject().get("code"));

		if (status.equalsIgnoreCase("PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO")) {
			logSuccess("Error code is PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO as expected");
		} else {
			throw error ("Incorrect error code "+ status +". Expected PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO");
		}
		return env;
	}
}
