package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractErrorFromJwtResponseCondition extends AbstractCondition {

	protected void validateError(JsonObject response, String errorToExpect) {
		int status = OIDFJSON.getInt(response.get("status"));
		switch(status) {
			case 422:
				try {
					JsonObject jwt = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(response.get("body")));
					JsonObject claims = jwt.getAsJsonObject("claims");
					JsonArray errors = claims.getAsJsonArray("errors");
					AtomicBoolean passed = new AtomicBoolean(false);
					errors.forEach(e -> {
						JsonObject error = (JsonObject) e;
						String errorCode = OIDFJSON.getString(error.get("code"));
						if(errorCode.equals(errorToExpect)) {
							passed.set(true);
						}
						if(!passed.get()) {
							throw error("Error code was not " + errorToExpect);
						} else {
							logSuccess("Successfully found error code  " + errorToExpect);
						}
					});

				} catch (ParseException e) {
					throw error("Could not parse JWT");
				}
				break;
			default:
				log("Response status was not 422 - not taking any action", Map.of("status", status));
				break;
		}
	}

}
