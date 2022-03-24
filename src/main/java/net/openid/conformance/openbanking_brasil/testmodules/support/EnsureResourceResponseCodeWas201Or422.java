package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class EnsureResourceResponseCodeWas201Or422 extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response_full");
		Integer status = (Integer) OIDFJSON.getNumber(response.get("status"));
		if(status != HttpStatus.UNPROCESSABLE_ENTITY.value() && status != HttpStatus.CREATED.value()) {
			log("Response status was not 201 or 422 as expected", Map.of("Status", status));
			throw error("Was expecting a 201 or 422 response");
		} else {
			logSuccess("Response status, as expected", Map.of("Status", status));
		}
		return env;
	}
}
