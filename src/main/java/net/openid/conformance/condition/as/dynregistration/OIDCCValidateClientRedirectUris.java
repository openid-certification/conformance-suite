package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.validation.RedirectURIValidationUtil;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Registration request must contain at least one redirect_uri
 *
 * These checks apply to authorization request validation as well
 */
public class OIDCCValidateClientRedirectUris extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		int validUriCount = 0;
		String redirectUriString = null;
		JsonArray redirectUrisArray = null;
		try {
			redirectUrisArray = getRedirectUris();
			if(redirectUrisArray==null) {
				throw error("redirect_uris is not set");
			}
		} catch (IllegalStateException ex) {
			throw error("redirect_uris is not encoded as an array", ex);
		}

		for(int i=0; i<redirectUrisArray.size(); i++) {
			try {
				redirectUriString = OIDFJSON.getString(redirectUrisArray.get(i));
				URI uri = new URI(redirectUriString);
				if(uri.getFragment()!=null) {
					appendError( "failure_reason", "Invalid redirect uri. URI includes a fragment component.",
								"details", args("invalid_uri", redirectUriString));
					continue;
				}
				//Web Clients using the OAuth Implicit Grant Type MUST only register URLs using the https scheme as redirect_uris;
				//they MUST NOT use localhost as the hostname
				if(isApplicationTypeWeb() && hasImplicitResponseTypes()) {
					if ("http".equalsIgnoreCase(uri.getScheme())) {
						appendError("failure_reason", "Web Clients using the OAuth Implicit Grant Type MUST " +
									"only register URLs using the https scheme as redirect_uris",
									"details", args("uri", redirectUriString));
						continue;
					}
					//they MUST NOT use localhost as the hostname
					if (RedirectURIValidationUtil.isLocalhost(uri.getHost())) {
						appendError("failure_reason", "Web Clients using the OAuth Implicit Grant " +
										"Type MUST not use localhost as the hostname",
									"details", args("uri", redirectUriString, "host", uri.getHost()));
						continue;
					}
				}
				if (isApplicationTypeNative() && "http".equalsIgnoreCase(uri.getScheme())) {
					if (!RedirectURIValidationUtil.isLocalhost(uri.getHost()))
					{
						//Authorization Servers MAY reject Redirection URI values using the http scheme,
						//other than the localhost case for Native Clients.
						//Note: python suite allows http when application type is native and hostname is localhost
						appendError("failure_reason", "http scheme is allowed only for native applications using localhost",
									"details", args("uri", uri));
						continue;
					}
				}
				validUriCount++;
			} catch (URISyntaxException e) {
				appendError("failure_reason", "Invalid redirect uri: " + e.getMessage(),
							"details", args("invalid_uri", redirectUriString));
			}
		}
		if(validationErrors.size()>0) {
			throw error("redirect_uris validation failed", args("errors", validationErrors));
		}
		if(validUriCount==0) {
			throw error("At least one redirect_uri is required in dynamic client registration requests.");
		}
		logSuccess("Valid redirect_uri(s) provided in registration request", args("redirect_uris", redirectUrisArray));
		return env;
	}
}
