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
 * only urn:brasil:openbanking:loa2 and/or urn:brasil:openbanking:loa3 ?
 */
public class FAPIBrazilValidateDefaultAcrValues extends AbstractClientValidationCondition {
	@Override
	@PreEnvironment(required = { "client", "server"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		try {
			JsonArray defaultAcrValues = getDefaultAcrValues();
			if(defaultAcrValues==null) {
				log("default_acr_values is not set");
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
