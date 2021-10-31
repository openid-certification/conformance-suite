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
//	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		int statusCode = env.getInteger("resource_endpoint_response_status");
//		String endpointName = env.getString("endpoint_response", "endpoint_name");

		if(statusCode == org.apache.http.HttpStatus.SC_OK) {
			logSuccess("endpoint returned an http status of 200 - ending test now", args("http_status", statusCode));
		}

		if(statusCode == org.apache.http.HttpStatus.SC_CREATED) {
			logSuccess("endpoint returned an http status of 2301 - proceeding with test now", args("http_status", statusCode));
			env.putString("proceed_with_test", "proceed");
		}

		if (statusCode != org.apache.http.HttpStatus.SC_CREATED && statusCode != org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY) {
			throw error("endpoint returned an unexpected http status - either 201 or 200 accepted", args("http_status", statusCode));
		}

		logSuccess("endpoint returned the expected http status", args("http_status", statusCode));

		return env;
	}
}
