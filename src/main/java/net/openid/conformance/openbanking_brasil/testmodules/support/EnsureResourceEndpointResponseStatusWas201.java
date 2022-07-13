package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResourceEndpointResponseStatusWas201 extends AbstractCondition {


	@Override
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");
		if(status == HttpStatus.CREATED.value()) {
			logSuccess("Valid response status returned");
		} else {
			throw error("Was expecting 201 Response", args("Received status", status));
		}
		return env;
	}



}
