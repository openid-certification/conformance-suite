package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.HttpUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * client_uri
 * OPTIONAL. URL of the home page of the Client. The value of this field MUST point to a valid Web page.
 * If present, the server SHOULD display this URL to the End-User in a followable fashion.
 * If desired, representation of this Claim in different languages and scripts is represented as
 * described in Section 2.1.
 *
 * This class just checks if the uri returns a http response < 400
 */
public class ValidateClientUris extends AbstractValidateUrisBasedOnHttpStatusCodeOnly
{

	@Override
	protected Map<String, String> getUrisToTest()
	{
		return getAllClientUris();
	}

	@Override
	protected String getMetadataName()
	{
		return "client_uri";
	}
}
