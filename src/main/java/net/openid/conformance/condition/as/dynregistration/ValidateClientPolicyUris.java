package net.openid.conformance.condition.as.dynregistration;

import java.util.Map;

/**
 * policy_uri
 * OPTIONAL. URL that the Relying Party Client provides to the End-User to read about the how
 * the profile data will be used. The value of this field MUST point to a valid web page.
 * The OpenID Provider SHOULD display this URL to the End-User if it is given.
 * If desired, representation of this Claim in different languages and scripts is represented
 * as described in Section 2.1.
 *
 * This class just checks if the uri returns a http response < 400
 */
public class ValidateClientPolicyUris extends AbstractValidateUrisBasedOnHttpStatusCodeOnly
{

	@Override
	protected Map<String, String> getUrisToTest() {
		return getAllPolicyUris();
	}

	@Override
	protected String getMetadataName() {
		return "policy_uri";
	}

}
