package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

import java.util.Map;

public abstract class AbstractConsentRejectionValidation extends AbstractCondition {

	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();

	protected boolean validateResponse(String response, String reason, String preDefinedRejectedBy){
		JsonObject consentResponse = GSON.fromJson(response,JsonObject.class);
		JsonObject body = consentResponse.getAsJsonObject("data");
		JsonObject rejection = body.getAsJsonObject("rejection");
		if (rejection == null){
			JsonElement statusElem = body.get("status");
			if (!statusElem.isJsonNull()) {
				if(OIDFJSON.getString(statusElem).equals("REJECTED")) {
					throw error("consent status is REJECTED but no rejection object found", args("body", body));
				}
			}
			return false;
		}
		JsonObject rejectionReason = rejection.getAsJsonObject("reason");

		if (rejectionReason == null){
			throw error("Rejection object did not contain reason object");
		}

		String code = OIDFJSON.getString(rejectionReason.get("code"));
		if (code == null || code.isEmpty()){
			throw error("Unable to find rejection code in rejection object");
		}
		logSuccess(code);

		String rejectedBy = OIDFJSON.getString(rejection.get("rejectedBy"));
		if (rejectedBy == null || rejectedBy.isEmpty()){
			throw error("Unable to find rejectedBy inside rejection object");
		}
		logSuccess(rejectedBy);

		if (reason.equals(code) && rejectedBy.equals(preDefinedRejectedBy)){
			logSuccess("Successfully found code and rejectedBy", Map.of("code",code,"rejectedBy",rejectedBy));
			return true;
		}
		return false;
	}
}
