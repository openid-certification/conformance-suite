package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class EnsureResponseCodeWas400or422or201 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");

		if(status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.UNPROCESSABLE_ENTITY.value() || status == HttpStatus.CREATED.value()){
			logSuccess("The response code was 400, 422 or 201 as expected", Map.of("Response code", status));
		}else {
			throw error("Was expecting either a 400, 422 or 201 response");
		}
		return env;
	}
}
