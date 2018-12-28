package io.fintechlabs.testframework.condition.client;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractImplicitHashToTokenEndpointResponse extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ExtractImplicitHashToTokenEndpointResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment() // We want an explicit error if implicit_hash is empty
	@PostEnvironment(required = { "callback_params", "token_endpoint_response" })
	public Environment evaluate(Environment env) {

		if (!Strings.isNullOrEmpty(env.getString("implicit_hash"))) {

			String hash = env.getString("implicit_hash").substring(1); // strip off the leading # character

			List<NameValuePair> parameters = URLEncodedUtils.parse(hash, Charset.defaultCharset());

			log("Extracted response from hash", args("parameters", parameters));

			JsonObject o = new JsonObject();
			for (NameValuePair pair : parameters) {
				o.addProperty(pair.getName(), pair.getValue());
			}

			// these count as both the authorization and token responses
			env.putObject("callback_params", o);
			env.putObject("token_endpoint_response", o);

			logSuccess("Extracted the hash values", o);

			return env;

		} else {
			throw error("Couldn't find the response in hash for implicit flow");
		}

	}

}
