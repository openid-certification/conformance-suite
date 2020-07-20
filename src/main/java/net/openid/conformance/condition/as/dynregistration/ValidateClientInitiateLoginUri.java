package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public class ValidateClientInitiateLoginUri extends AbstractCondition
{
	@Override
	@PreEnvironment(required = {"client"})
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");

		if (!client.has("initiate_login_uri")) {
			throw error("Client configuration does not contain required 'initiate_login_uri'");
		}

		JsonElement initiateLoginUri = client.get("initiate_login_uri");
		URL url;
		try {
			url = new URL(OIDFJSON.getString(initiateLoginUri));
		} catch (MalformedURLException invalidURL) {
			throw error("initiate_login_uri does not contain a valid URL",
				args("initiate_login_uri", initiateLoginUri));
		}

		if (!url.getProtocol().equals("https")) {
			throw error("initiate_login_uri does not use https",
				args("initiate_login_uri", initiateLoginUri));
		}

		logSuccess("valid initiate_login_uri", args("initiate_login_uri", url.toString()));

		return env;
	}
}
