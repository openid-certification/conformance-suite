package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 *  default_max_age
 *  OPTIONAL. Default Maximum Authentication Age. Specifies that the End-User MUST be actively
 *  authenticated if the End-User was authenticated longer ago than the specified number of seconds.
 *  The max_age request parameter overrides this default value. If omitted, no default
 *  Maximum Authentication Age is specified.
 *
 */
public class ValidateDefaultMaxAge extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		try {
			Number defaultMaxAge = getDefaultMaxAge();
			if(defaultMaxAge==null) {
				logSuccess("default_max_age is not set");
				return env;
			} else {
				logSuccess("default_max_age is encoded as a number",
							args("default_max_age", defaultMaxAge));
				return env;
			}
		} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
			throw error("default_max_age is not encoded as a number",
						args("default_max_age", client.get("default_max_age")));
		}
	}
}
