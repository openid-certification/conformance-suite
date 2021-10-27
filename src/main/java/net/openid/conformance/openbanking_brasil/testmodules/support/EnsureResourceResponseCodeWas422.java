package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class EnsureResourceResponseCodeWas422 extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		Integer status = env.getInteger("resource_endpoint_response_status");
		if(status != HttpStatus.UNPROCESSABLE_ENTITY.value()) {
			log("Response status was not 422 as expected", Map.of("status", status));
			throw error("Was expecting a 422 response");
		} else {
			logSuccess("422 response status, as expected");
		}
		return env;
	}
}