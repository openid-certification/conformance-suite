package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilExtractConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	//also adds consent_request_cnpj if found in request
	@PostEnvironment(strings = {"consent_request_cpf"}, required = {"new_consent_request"})
	public Environment evaluate(Environment env) {

		JsonElement bodyJson = env.getElementFromObject("incoming_request", "body_json");
		if (bodyJson == null) {
			throw error("Request must contain a JSON structure in the body", args("expected", "JSON", "actual", bodyJson));
		}

		JsonObject parsedRequest = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();

		if(!parsedRequest.has("data")) {
			throw error("Request must contain a 'data' element", args("request_json", parsedRequest));
		}
		JsonObject data = parsedRequest.get("data").getAsJsonObject();
		if(!data.has("permissions")) {
			throw error("'data' object must contain a 'permissions' element", args("request_json", parsedRequest));
		}
		if(!data.get("permissions").isJsonArray()) {
			throw error("'permissions' must be an array", args("request_json", parsedRequest));
		} else {
			JsonArray permissions = data.get("permissions").getAsJsonArray();
			if (permissions.size() < 1 || permissions.size() > 30) {
				throw error("'permissions' must contain at least 1 entry and cannot have more than 30 entries",
					args("permissions", permissions, "size", permissions.size()));
			}
		}

		boolean hasCnpj = false;
		boolean hasCpf = false;
		if(data.has("loggedUser")) {
			JsonObject loggedUser = data.get("loggedUser").getAsJsonObject();

			JsonObject loggedUserDocument = loggedUser.get("document").getAsJsonObject();
			if(!loggedUserDocument.has("rel") || !"CPF".equals(OIDFJSON.getString(loggedUserDocument.get("rel")))) {
				throw error("loggedUser.document.rel is not equal to 'CPF'", args("loggedUser", loggedUser));
			}
			String identification = OIDFJSON.getString(loggedUserDocument.get("identification"));
			env.putString("consent_request_cpf", identification);
			hasCpf = true;
		}

		if(data.has("businessEntity")) {
			JsonObject businessEntity = data.get("businessEntity").getAsJsonObject();
			JsonObject businessEntityDocument = businessEntity.get("document").getAsJsonObject();
			if(!businessEntityDocument.has("rel") || !"CNPJ".equals(OIDFJSON.getString(businessEntityDocument.get("rel")))) {
				throw error("businessEntity.document.rel is not equal to 'CNPJ'", args("businessEntity", businessEntity));
			}
			String businessIdentification = OIDFJSON.getString(businessEntityDocument.get("identification"));
			env.putString("consent_request_cnpj", businessIdentification);
			hasCnpj = true;
		}
		if(!hasCnpj && !hasCpf) {
			throw error("Either 'CPF' or 'CNPJ' must be provided");
		}

		logSuccess("Consent successfully extracted from the request.", parsedRequest);

		env.putObject("new_consent_request", parsedRequest);
		return env;

	}

}
