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

public class ExtractImplicitHashToCallbackResponse extends AbstractCondition {

	public ExtractImplicitHashToCallbackResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(strings = "implicit_hash")
	@PostEnvironment(required = "callback_params")
	public Environment evaluate(Environment env) {
		String implicit_hash = env.getString("implicit_hash");
		if (!Strings.isNullOrEmpty(implicit_hash)) {

			String hash = implicit_hash.substring(1); // strip off the leading # character

			List<NameValuePair> parameters = URLEncodedUtils.parse(hash, Charset.defaultCharset());

			log("Extracted response from URL fragment", args("parameters", parameters));

			JsonObject o = new JsonObject();
			for (NameValuePair pair : parameters) {
				o.addProperty(pair.getName(), pair.getValue());
			}

			env.putObject("callback_params", o);

			logSuccess("Extracted the hash values", o);

			return env;

		}

		JsonObject o = new JsonObject();
		env.putObject("callback_params", o);
		logSuccess("implicit_hash is empty", o);

		return env;
	}

}
