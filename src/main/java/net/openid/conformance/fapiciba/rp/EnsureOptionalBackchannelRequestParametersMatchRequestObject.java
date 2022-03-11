package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashMap;
import java.util.Map;

/**
 * Use this condition for logging a WARNING only
 * Normally this condition should never lead to a failure
 */
public class EnsureOptionalBackchannelRequestParametersMatchRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"backchannel_endpoint_http_request_params", "backchannel_request_object"})
	public Environment evaluate(Environment env) {

		//we loop over all parameters and add a log entry if they are not equal
		JsonObject authzEndpointReqParams = env.getObject("backchannel_endpoint_http_request_params");
		JsonObject requestObjectClaims = env.getElementFromObject("backchannel_request_object", "claims").getAsJsonObject();

		Map<String, Object> argsForLog = new HashMap<>();

		for (String paramName : authzEndpointReqParams.keySet()) {
			if(EnsureRequiredAuthorizationRequestParametersMatchRequestObject.parametersThatMustMatch.contains(paramName)) {
				//these should be already checked. checking again would cause duplicate logs
				continue;
			}
			if(requestObjectClaims.has(paramName)) {
				//scope=openid special case. We don't log a warning when scope http request parameter equals openid,
				//only when it is exactly "openid".
				//We will log a warning when it is "openid xyz" in http request but "openid xyz abc" in request object
				if("scope".equals(paramName)) {
					String scopeValue = OIDFJSON.getString(authzEndpointReqParams.get(paramName));
					if("openid".equals(scopeValue)) {
						continue;
					}
				}
				if(!authzEndpointReqParams.get(paramName).equals(requestObjectClaims.get(paramName))) {
					argsForLog.put(paramName, args("Value in http request", authzEndpointReqParams.get(paramName),
													"Value in request object", requestObjectClaims.get(paramName))
					);
				}
			}
		}

		if(argsForLog.isEmpty()) {
			logSuccess("All http request parameters and request object claims match");
			return env;
		}
		throw error("Some http request parameters and request object claims do not match. " +
			"This is allowed but you should check if the differences are intentional", argsForLog);
	}

}
