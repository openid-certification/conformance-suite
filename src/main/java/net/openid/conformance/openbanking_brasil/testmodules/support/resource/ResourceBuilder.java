package net.openid.conformance.openbanking_brasil.testmodules.support.resource;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceBuilder extends AbstractCondition {
	protected String api;
	protected String endpoint;

	@Override
	public Environment evaluate(Environment env) {

		String url = env.getString("config", "resource.resourceUrl");

		String fullResourceUrlRegex = "^(https://)(.*?)(" + api + "/v[0-9])(.*?)";
		if(!url.matches(fullResourceUrlRegex)) {
			throw error("Base url path has not been correctly provided. It must match the regex " + fullResourceUrlRegex);
		}

		String specificResourceUrlRegex = "^(https://)(.*?)(" + api + "/v[0-9])" + endpoint;
		if(url.matches(specificResourceUrlRegex)) {
			logSuccess("Nothing to be done, resource.resourceUrl already contains the correct endpoint");
		}

		Pattern p = Pattern.compile("^(https://)(.*)(" + api + "/v[0-9])");
		Matcher m = p.matcher(url);
		String baseUrl;
		if(m.find()) {
			baseUrl = m.group(0);
			String protectedUrl = baseUrl + endpoint;
			env.putString("protected_resource_url", protectedUrl);
			logSuccess("Endpoint added", args(endpoint, protectedUrl));
		} else{
			//This else will only be valid if something goes wrong with the Matcher object
			throw error("Base url path has not been correctly provided. It must match the regex " + fullResourceUrlRegex);
		}

		return env;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
}
