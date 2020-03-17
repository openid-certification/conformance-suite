package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * initiate_login_uri
 *  OPTIONAL. URI using the https scheme that a third party can use to initiate a
 *  login by the RP, as specified in Section 4 of OpenID Connect Core 1.0 [OpenID.Core].
 *  The URI MUST accept requests via both GET and POST.
 *  The Client MUST understand the login_hint and iss parameters and SHOULD support the
 *  target_link_uri parameter.
 *
 *  Just checks if it is a valid https uri or not
 *
 */
public class ValidateInitiateLoginUri extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		try {
			String initiateLoginUri = getInitiateLoginUri();
			if(initiateLoginUri==null) {
				logSuccess("initiate_login_uri is not set");
				return env;
			} else {
				try {
					URI uri = new URI(initiateLoginUri);
					if("https".equalsIgnoreCase(uri.getScheme())) {
						logSuccess("initiate_login_uri is valid",
									args("initiate_login_uri", initiateLoginUri));
						return env;
					}
					throw error("initiate_login_uri is not a https URI",
						args("initiate_login_uri", initiateLoginUri));
				} catch (URISyntaxException e) {
					throw error("initiate_login_uri is not a valid URI",
								args("initiate_login_uri", initiateLoginUri));
				}
			}
		} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
			throw error("initiate_login_uri is not encoded as a string",
						args("initiate_login_uri", client.get("initiate_login_uri")));
		}
	}
}
