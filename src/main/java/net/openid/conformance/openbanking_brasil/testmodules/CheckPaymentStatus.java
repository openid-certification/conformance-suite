package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Base64;

public class CheckPaymentStatus extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource_endpoint_response_full");
		if(OIDFJSON.getInt(resource.get("status")) == 200){
			logSuccess("Resource correctly returnned 200");
		} else {
			logFailure("Resource failed to correctly return 200: " + OIDFJSON.getInt(resource.get("status")));
		}

		String jwtBody = OIDFJSON.getString(resource.get("body")).split("\\.")[1];
		String decodedBody = new String(Base64.getUrlDecoder().decode(jwtBody.getBytes()));
		JsonObject body = new JsonParser().parse(decodedBody).getAsJsonObject();
		body = body.getAsJsonObject("data");
		String paymentStatus = OIDFJSON.getString(body.get("status"));
		if(paymentStatus.equals("ACCC") || paymentStatus.equals("RJCT")){
			env.putBoolean("paymentStatusCorrect", true);
		} else {
			env.putBoolean("paymentStatusCorrect", false);
		}
		return env;
	}
}
