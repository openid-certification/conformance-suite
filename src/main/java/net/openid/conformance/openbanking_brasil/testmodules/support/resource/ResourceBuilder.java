package net.openid.conformance.openbanking_brasil.testmodules.support.resource;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * A condition that builds a protected_resource_url from the resource.resourceUrl provided in the config field.
 *
 * The standard use of this class is done when the attribute allowDifferentBaseUrl is set to false, which means
 * the base API will be preserved and a new endpoint will be added. In order to build the resourceUrl, use
 * BuildXConfigFromConsentUrl where X is a chosen API.
 *
 * By setting alllowDifferentBaseUrl to true, the API can also be changed and in that case the consentUrl will be used
 * as entry point.
 *
 */
public class ResourceBuilder extends AbstractCondition {
	protected String api;
	protected String endpoint;
	protected boolean allowDifferentBaseUrl = false;

	@Override
	public Environment evaluate(Environment env) {

		String url = env.getString("config", "resource.resourceUrl");

		if (allowDifferentBaseUrl) {
			url = env.getString("config","resource.consentUrl").replaceFirst("consents", api);
		}

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

	public void setAllowDifferentBaseUrl(boolean allowDifferentBaseUrl){
		this.allowDifferentBaseUrl = allowDifferentBaseUrl;
	}
}
