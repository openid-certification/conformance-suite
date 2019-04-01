//Author: ddrysdale

package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

// author: ddrysdale

public class CheckDiscEndpointDiscoveryUrl extends AbstractCondition {

	private final String requiredProtocol = "https";

	private final String environmentBaseObject = "config";
	private final String environmentVariable = "server.discoveryUrl";


	private final String errorMessageNotJsonPrimitive = "Specified value is not a Json primitive";
	private final String errorMessageInvalidURL = "Invalid URL. Unable to parse.";
	private final String errorMessageNotRequiredProtocol = "Expected " + requiredProtocol + " protocol for " + environmentVariable;



	public CheckDiscEndpointDiscoveryUrl(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement configUrl = env.getElementFromObject(environmentBaseObject, environmentVariable);
		if ( configUrl == null ) {
			logFailure("Unable to find Discovery URL", args("No discoveryUrl", env.getObject("config")));
		} else  {
			if (!configUrl.isJsonPrimitive()) {
				throw error(errorMessageNotJsonPrimitive, args("Failure", configUrl));
			} else {
				try {
					URL extractedUrl = new URL(configUrl.getAsString());
					if ( !extractedUrl.getProtocol().equals(requiredProtocol)) {
						throw error(errorMessageNotRequiredProtocol, args("actual", extractedUrl.getProtocol(), "expected",requiredProtocol));
					}

					logSuccess("discoveryUrl", args("actual",configUrl));

				} catch (MalformedURLException invalidURL) {
					throw error(errorMessageInvalidURL,args("Failure", configUrl));
				}
			}
		}
		return env;
	}
}
