package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Joiner;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;
import java.util.StringJoiner;

public class BackchannelRequestIsFormDataCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("backchannel_endpoint_http_request", "headers.content-type");
		List<String> allowedContentTypes =
			List.of("application/x-www-form-urlencoded", "application/x-www-form-urlencoded;charset=utf-8");

		if(allowedContentTypes.stream().noneMatch(s -> s.equalsIgnoreCase(contentType))) {
			throw error("Content-Type must be one of '" + Joiner.on("', '").join(allowedContentTypes) + "'");
		}

		logSuccess("Backchannel authentication request has Content-Type 'application/x-www-form-urlencoded'");

		return env;
	}


}
