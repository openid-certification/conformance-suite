package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

/**
 * default_acr_values
 *  OPTIONAL. Default requested Authentication Context Class Reference values.
 *  Array of strings that specifies the default acr values that the OP is being
 *  requested to use for processing requests from this Client, with the values
 *  appearing in order of preference. The Authentication Context Class satisfied
 *  by the authentication performed is returned as the acr Claim Value in the
 *  issued ID Token. The acr Claim is requested as a Voluntary Claim by this
 *  parameter. The acr_values_supported discovery element contains a list of the
 *  supported acr values supported by this server. Values specified in the acr_values
 *  request parameter or an individual acr Claim request override these default values.
 *
 */
public class ValidateDefaultAcrValues extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client", "server"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		try {
			JsonArray defaultAcrValues = getDefaultAcrValues();
			if(defaultAcrValues==null) {
				logSuccess("default_acr_values is not set");
				return env;
			}
			Set<String> acrValuesSupported = null;
			JsonElement acrValuesSupportedJsonElement = env.getElementFromObject("server", "acr_values_supported");
			if(acrValuesSupportedJsonElement!=null && acrValuesSupportedJsonElement.isJsonArray()) {
				acrValuesSupported = new HashSet<>();
				for(JsonElement acrElement : acrValuesSupportedJsonElement.getAsJsonArray()) {
					acrValuesSupported.add(OIDFJSON.getString(acrElement));
				}
			}

			for(JsonElement element : defaultAcrValues) {
				try {
					String acrValue = OIDFJSON.getString(element);
					//check if acrValue is one of acr_values_supported if we returned one
					if(acrValuesSupported!=null) {
						if(!acrValuesSupported.contains(acrValue)) {
							throw error("acr value is not one of the supported ones",
										args("acr_values_supported", acrValuesSupportedJsonElement,
												"offending_value", acrValue));
						}
					}
				} catch (OIDFJSON.UnexpectedJsonTypeException unexpectedTypeEx) {
					throw error("default_acr_values contains a value that is not encoded as a string",
								args("element", element));
				}
			}
			logSuccess("default_acr_values is valid", args("default_acr_values", defaultAcrValues));
			return env;

		} catch (IllegalStateException ex) {
			throw error("default_acr_values is not encoded as a json array",
						args("default_acr_values", client.get("default_acr_values")));
		}
	}
}
