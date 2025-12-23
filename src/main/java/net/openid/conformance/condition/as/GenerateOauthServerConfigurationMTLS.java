package net.openid.conformance.condition.as;

import com.google.common.base.Strings;

import java.net.MalformedURLException;
import java.net.URL;

public class GenerateOauthServerConfigurationMTLS extends GenerateServerConfigurationMTLS {

	@Override
	protected String getDiscoveryUrl(String baseUrl) {
		try {
			URL url = new URL(baseUrl);
			if(!Strings.isNullOrEmpty(url.getPath())) {
				int foundIndex = baseUrl.indexOf(url.getPath());
				if(foundIndex != -1) {
					return baseUrl.substring(0, foundIndex) + "/.well-known/oauth-authorization-server" + url.getPath();
				}
			}
			return baseUrl + "./well-known/oauth-authorization-server";
		} catch (MalformedURLException e) {
			throw error("Invalid URL", e, args("baseUrl", baseUrl));
		}
	}

}
