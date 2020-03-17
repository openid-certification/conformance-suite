package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 *  require_auth_time
 *  OPTIONAL. Boolean value specifying whether the auth_time Claim in the ID Token is REQUIRED.
 *  It is REQUIRED when the value is true. (If this is false, the auth_time Claim can still be
 *  dynamically requested as an individual Claim for the ID Token using the claims request
 *  parameter described in Section 5.5.1 of OpenID Connect Core 1.0 [OpenID.Core].) If omitted,
 *  the default value is false.
 *
 */
public class ValidateRequireAuthTime extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		try {
			Boolean requireAuthTime = getRequireAuthTime();
			if(requireAuthTime==null) {
				logSuccess("require_auth_time is not set");
				return env;
			} else {
				logSuccess("require_auth_time is encoded as a boolean",
							args("require_auth_time", requireAuthTime));
				return env;
			}
		} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
			throw error("require_auth_time is not encoded as a boolean",
						args("require_auth_time", client.get("require_auth_time")));
		}
	}
}
