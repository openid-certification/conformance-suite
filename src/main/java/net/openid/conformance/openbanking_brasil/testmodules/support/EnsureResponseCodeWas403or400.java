package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas403or400 extends AbstractCondition {
	@Override
	@PostEnvironment(strings = "warning_message")
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");
		if(status == HttpStatus.BAD_REQUEST.value()) {
			logSuccess("400 response status returned");
		} else if (status == HttpStatus.FORBIDDEN.value()){
			env.putString("warning_message", "Participant returned a 403 this is accepted behaviour in the specs bit awaiting clarification if this is correct");
		} else {
			throw error("Was expecting either a 403 or a 401 response");
		}
		return env;
	}
}
