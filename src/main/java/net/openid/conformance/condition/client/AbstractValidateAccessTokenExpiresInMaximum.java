package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates that the access token lifetime returned in expires_in is positive and no longer
 * than the maximum the ecosystem allows. Subclasses supply the maximum.
 */
public abstract class AbstractValidateAccessTokenExpiresInMaximum extends AbstractCondition {

	/**
	 * @return the largest access token lifetime, in seconds, this profile permits.
	 */
	protected abstract int getMaximumSeconds();

	@Override
	@PreEnvironment(strings = {}, required = "expires_in")
	public Environment evaluate(Environment env) {

		int maximumSeconds = getMaximumSeconds();

		JsonObject expiresIn = env.getObject("expires_in");
		JsonElement je = expiresIn.get("expires_in");
		try {
			JsonPrimitive jp = je.getAsJsonPrimitive();
			if (!jp.isNumber()) {
				throw error("expires_in is not a number");
			}

			Number n = OIDFJSON.getNumber(jp);
			if (n.intValue() <= 0) {
				throw error("expires_in is less than or equal zero");
			}

			if (n.intValue() > maximumSeconds) {
				throw error("expires_in greater than " + maximumSeconds + " seconds",
					args("expires_in", jp, "maximum_seconds", maximumSeconds));
			}

		} catch (IllegalStateException ex) {
			throw error("expires_in is not a JSON primitive");
		}

		logSuccess("expires_in no greater than " + maximumSeconds + " seconds", expiresIn);
		return env;

	}

}
