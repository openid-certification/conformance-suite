package net.openid.conformance.condition.client;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckRedirectUri extends AbstractCondition {

	@PreEnvironment(strings = "redirect_uri")
	@Override
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("redirect_uri");

		try {

			URI uri = new URI(redirectUri);

			if (uri.getScheme().equals("http")) {
				// make sure that it's a "localhost" URL
				InetAddress addr = InetAddress.getByName(uri.getHost());

				if (!addr.isLoopbackAddress()) {
					throw error("Address given was not a loopback (localhost) address", args("scheme", uri.getScheme(), "host", uri.getHost()));
				}

				logSuccess("Plain http on localhost allowed", args("scheme", uri.getScheme(), "host", uri.getHost()));
				return env;

			} else if (uri.getScheme().equals("https")) {
				// any remote host URL is fine
				logSuccess("Encrypted http on any host allowed", args("scheme", uri.getScheme(), "host", uri.getHost()));
				return env;

			} else {
				// a non-HTTP URL is assumed to be app-specific
				logSuccess("Non-http URL allowed, assuming app-specific", args("scheme", uri.getScheme(), "path", uri.getSchemeSpecificPart()));
				return env;
			}
		} catch (URISyntaxException | UnknownHostException e) {
			throw error("Couldn't parse key as URI", e, args("uri", redirectUri));
		}

	}

}
