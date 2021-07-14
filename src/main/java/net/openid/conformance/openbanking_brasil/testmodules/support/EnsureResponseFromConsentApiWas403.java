package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpStatus;

public class EnsureResponseFromConsentApiWas403 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "errored_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("errored_response");
		int status = OIDFJSON.getInt(response.get("status_code"));
		if(status != HttpStatus.FORBIDDEN.value()) {
			throw error("Was expecting a 403 response");
		} else {
			logSuccess("403 response status, as expeted");
		}
		return env;
	}

}
