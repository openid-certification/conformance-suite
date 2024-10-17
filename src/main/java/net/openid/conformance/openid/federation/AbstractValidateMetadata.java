package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractValidateMetadata extends AbstractCondition {

	public boolean validateUrl(String urlString) {
		try {
			URL url = new URL(urlString);

			if (!"https".equals(url.getProtocol())) {
				return false;
			}

			// Fragment
			if (url.getRef() != null) {
				return false;
			}

			// Ensure path and query parameters are in application/x-www-form-urlencoded format
			// This regex checks for allowed characters in paths and query parameters
			String urlEncodedRegex = "^[a-zA-Z0-9-._~!$&'()*+,;=:@/%]*$";
			Pattern pattern = Pattern.compile(urlEncodedRegex);
			Matcher pathMatcher = pattern.matcher(url.getPath());
			Matcher queryMatcher = pattern.matcher(url.getQuery() != null ? url.getQuery() : "");

			return pathMatcher.matches() && queryMatcher.matches();

		} catch (MalformedURLException e) {
			return false;
		}
	}

	public boolean validateUrl(JsonObject parent, String key, boolean optional) {
		JsonElement value = parent.get(key);
		if (value == null && optional) {
			return true;
		}
		String urlString = OIDFJSON.getString(value);
		return validateUrl(urlString);
	}

}
