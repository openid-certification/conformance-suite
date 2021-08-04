package net.openid.conformance.apis;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class allows users to optionally provide simply an API base url as a resource url
 * If there is no path on the URL, subclasses of this will append the correct resource url
 * If they provide a full URL, we don't change it
 */
public abstract class AbstractProtectedResourceInferenceCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("resource", "resourceUrl");
		URL url = null;
		try {
			url = new URL(baseUrl);
		} catch (MalformedURLException e) {
			throw error("The configured resource URL: '" + baseUrl + "' was not a URL");
		}
		String path = url.getPath();
		if(path.equals("")) {
			baseUrl = baseUrl.concat(getResourcePath());
			doLog(baseUrl);
		} else
		if(path.equals("/")) {
			baseUrl = StringUtils.chop(baseUrl).concat(getResourcePath());
			doLog(baseUrl);
		} else {
			logSuccess("Configured url is already a full resource url - leaving alone");
		}
		env.putString("protected_resource_url", baseUrl);
		return env;
	}

	private void doLog(String baseUrl) {
		logSuccess("Set API base URL to specific resource url", args("configured url", baseUrl));
	}

	protected abstract String getResourcePath();
}
