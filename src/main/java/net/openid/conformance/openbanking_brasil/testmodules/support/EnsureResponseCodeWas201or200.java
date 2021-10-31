package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class EnsureResponseCodeWas201or200 extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response_full");
		Integer status = (Integer) OIDFJSON.getNumber(response.get("status"));
		if(status != HttpStatus.OK.value() || status != HttpStatus.CREATED.value()) {
			log("Response status was not 201 or 422 as expected", Map.of("status", status));
			throw error("Was expecting a 201 or 422 response");
		} else if (status == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
			logSuccess("422 response status, as expected");
		} else {
			logSuccess("201 response status, as expected");
		}
		return env;
	}
}
