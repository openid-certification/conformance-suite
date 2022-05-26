package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBusinessEntityInConsentRequest extends AbstractCondition {
	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate (Environment env) {
		JsonObject data = env.getElementFromObject("consent_endpoint_request", "data").getAsJsonObject();
		if(data == null) {
			throw error("Data object is missing in consent_endpoint_request");
		}

		JsonObject businessEntity = data.getAsJsonObject("businessEntity");
		if(businessEntity == null) {
			throw error("businessEntity object is missing in data object");
		}
		logSuccess("businessEntity was successfully found.", args("businessEntity", businessEntity));
		return env;
	}
}
