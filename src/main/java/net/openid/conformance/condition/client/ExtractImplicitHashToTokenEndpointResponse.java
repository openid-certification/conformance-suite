package net.openid.conformance.condition.client;

import java.nio.charset.Charset;
import java.util.List;

import net.openid.conformance.testmodule.Environment;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;

public class ExtractImplicitHashToTokenEndpointResponse extends AbstractCondition {

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
