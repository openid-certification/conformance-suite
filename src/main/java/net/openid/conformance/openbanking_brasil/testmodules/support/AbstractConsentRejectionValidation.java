package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
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
		JsonElement rejectionReasonElem = rejection.get("reason");

		if (rejectionReasonElem == null){
			throw error("Rejection object did not contain reason object");
		}

		if(!rejectionReasonElem.isJsonObject()) {
			throw error("Reason object is not a json object, it should have the mandatory field code");
		}

		JsonObject rejectionReason = rejectionReasonElem.getAsJsonObject();
		String code = null;
		if(rejectionReason.has("code")) {
			code = OIDFJSON.getString(rejectionReason.get("code"));
		}

		if (Strings.isNullOrEmpty(code)){
			throw error("Unable to find rejection code in rejection object");
		}
		logSuccess(code);

		if (!rejection.has("rejectedBy")) {
			throw error("Unable to find rejectedBy inside rejection object");
		}
		String rejectedBy = OIDFJSON.getString(rejection.get("rejectedBy"));
		if (Strings.isNullOrEmpty(rejectedBy)){
			throw error("RejectedBy is not a string inside rejection object");
		}
		logSuccess(rejectedBy);

		if (reason.equals(code) && rejectedBy.equals(preDefinedRejectedBy)){
			logSuccess("Successfully found code and rejectedBy", Map.of("code",code,"rejectedBy",rejectedBy));
			return true;
		}
		return false;
	}
}
