package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class BackchannelRequestRequestedExpiryIsAnInteger extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {

		JsonElement requestedExpiryClaim = env.getElementFromObject("backchannel_request_object", "claims.requested_expiry");
		if (requestedExpiryClaim == null) {
			logSuccess("Backchannel authentication request does not contain optional parameter 'requested_expiry'");
			return env;
		}

		try {

			Number requestedExpiryNumber = OIDFJSON.forceConversionToNumber(requestedExpiryClaim);
			int requestedExpiry = Integer.parseInt(requestedExpiryNumber.toString());
			if(requestedExpiry <= 0) {
				throw error("The 'requested_expiry' parameter must be a positive integer when present", args("requested_expiry", requestedExpiryClaim));
			}

			env.putInteger("requested_expiry", requestedExpiry);
			logSuccess("Backchannel authentication request contains valid parameter 'requested_expiry'", args("requested_expiry", requestedExpiryClaim));
			return env;

		} catch (OIDFJSON.ValueIsJsonNullException e) {
			throw error("requested_expiry must not be JSON null", args("requested_expiry", requestedExpiryClaim));
		} catch (OIDFJSON.UnexpectedJsonTypeException | NumberFormatException e) {
			throw error("requested_expiry must be an integer or a string representing an integer", args("requested_expiry", requestedExpiryClaim));
		}
	}
}
