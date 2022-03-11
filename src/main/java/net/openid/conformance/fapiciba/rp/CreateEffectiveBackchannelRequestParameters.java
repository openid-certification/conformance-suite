package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Merges http request parameters and request object parameters
 * and creates effective_authorization_endpoint_request environment entry
 */
public class CreateEffectiveBackchannelRequestParameters extends AbstractCondition {

	public static final String ENV_KEY = "effective_authorization_endpoint_request";

	//always use these constants to get values. just in case the keys, JsonObject structure change etc
	public static final String MAX_AGE = "max_age";
	public static final String PROMPT = "prompt";
	public static final String STATE = "state";
	public static final String REDIRECT_URI = "redirect_uri";
	public static final String RESPONSE_TYPE = "response_type";
	public static final String CLIENT_ID = "client_id";
	public static final String SCOPE = "scope";
	public static final String NONCE = "nonce";

	//WARNING "authorization_request_object" is also used but it's not required
	@Override
	@PreEnvironment(required = {"backchannel_endpoint_http_request_params"})
	@PostEnvironment(required = {ENV_KEY})
	public Environment evaluate(Environment env) {

		JsonObject authzEndpointReqParams = env.getObject("backchannel_endpoint_http_request_params");
		JsonObject effective = authzEndpointReqParams.deepCopy();
		effective.remove("request_uri");

		//override request parameters if authorization_request_object exists
		if(env.containsObject("backchannel_request_object")) {
			JsonObject requestObjectClaims = env.getElementFromObject("backchannel_request_object", "claims").getAsJsonObject();

			for (String paramName : requestObjectClaims.keySet()) {
				//TODO do JsonNull values require special handling?
				effective.add(paramName, requestObjectClaims.get(paramName));
			}
		}
		//numeric values handling. (for now only max_age)
		for(String claimName : EnsureNumericRequestObjectClaimsAreNotNull.numericClaimNames) {
			if(effective.has(claimName)) {
				JsonElement claimJsonElement = effective.get(claimName);
				try {
					Number claimAsNumber = OIDFJSON.forceConversionToNumber(claimJsonElement);
					effective.addProperty(claimName, claimAsNumber);
				} catch (OIDFJSON.ValueIsJsonNullException ex) {
					//value is json null. remove the entry from effective to prevent errors
					//EnsureNumericRequestObjectClaimsAreNotNull should be called to log a warning
					effective.remove(claimName);
					log(claimName + " has a json null value. Not including "+claimName+" in effective authorization endpoint request");
				} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
					throw error("Unexpected parameter value. Value is not encoded as a number.", args(claimName, claimJsonElement));
				}
			}
		}
		env.putObject(ENV_KEY, effective);
		logSuccess("Merged http request parameters with request object claims", args(ENV_KEY, effective));
		return env;
	}

}
