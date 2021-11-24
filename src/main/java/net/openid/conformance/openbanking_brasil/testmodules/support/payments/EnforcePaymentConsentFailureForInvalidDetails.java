package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnforcePaymentConsentFailureForInvalidDetails extends AbstractExtractJWT {

	public static final String EXPECTED_ERROR = "DETALHE_PGTO_INVALIDO";

	@Override
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("consent_endpoint_response_full");
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
						if(errorCode.equals(EXPECTED_ERROR)) {
							passed.set(true);
						}
						if(!passed.get()) {
							throw error("Error code was not " + EXPECTED_ERROR);
						} else {
							logSuccess("Successfully found error code  " + EXPECTED_ERROR);
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
		return env;
	}
}
