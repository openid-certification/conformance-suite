package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Base for checks on the {@code err} code of a receiver's push delivery error response
 * (RFC 8935 2.3). A response without a usable {@code err} member is not graded here - that
 * defect is reported by {@link OIDSSFValidatePushDeliveryErrorResponse}.
 */
public abstract class AbstractOIDSSFPushDeliveryErrorCodeCheck extends AbstractCondition {

	/**
	 * The {@code err} member of the JSON error body in {@code endpoint_response}, or
	 * {@code null} when the response carries no JSON object body with a string {@code err}.
	 */
	protected String getErrorCode(Environment env) {
		JsonElement bodyJson = env.getElementFromObject("endpoint_response", "body_json");
		if (bodyJson == null || !bodyJson.isJsonObject()) {
			return null;
		}
		JsonElement err = bodyJson.getAsJsonObject().get("err");
		if (err == null || !err.isJsonPrimitive() || !err.getAsJsonPrimitive().isString()) {
			return null;
		}
		return OIDFJSON.getString(err);
	}
}
