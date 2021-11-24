package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.Map;

public class VerifyRejectionReasonForQres extends AbstractCondition {

	public static final String EXPECTED_REASON = "ELEMENT_CONTENT_FORMALLY_INCORRECT";

	@Override
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("resource_endpoint_response_full");

		int statusCode = OIDFJSON.getInt(response.get("status"));
		switch(statusCode) {
			case 200:
				try {
					JsonObject jwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(response.get("body")));
					JsonObject claims = jwt.getAsJsonObject("claims");
					JsonObject data = claims.getAsJsonObject("data");
					String status = OIDFJSON.getString(data.get("status"));
					JsonElement rejectionReasonElement = data.get("rejectionReason");
					String rejectionReason = rejectionReasonElement == null ? null : OIDFJSON.getString(data.get("rejectionReason"));

					if(status.equals("RJCT")) {
						validateRejectionReason(rejectionReason);
					}
				} catch (ParseException e) {
					throw error("Could not parse JWT");
				}
				break;
			default:
				log("Response status was not 200 - not taking any action", Map.of("status", statusCode));
				break;
		}

		return env;
	}

	private void validateRejectionReason(String rejectionReason) {
		if(rejectionReason == null) {
			throw error("Rejection reason was not returned");
		}
		if(!EXPECTED_REASON.equals(rejectionReason)){
			throw error("Rejection reason was not correct", Map.of("reason", rejectionReason));
		}
	}


}
