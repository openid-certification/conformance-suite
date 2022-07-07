package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureConsentResponseWas400 extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		int status;
		try {
			status = env.getInteger("resource_endpoint_response_status");
		}catch (NullPointerException e) {
			status = env.getInteger("consent_endpoint_response_full", "status");
		}
		if(status != HttpStatus.BAD_REQUEST.value() ) {
			throw error("Was expecting a 400 response");
		} else {
			logSuccess("400 response status, as expected");
		}
		return env;
	}
	}

