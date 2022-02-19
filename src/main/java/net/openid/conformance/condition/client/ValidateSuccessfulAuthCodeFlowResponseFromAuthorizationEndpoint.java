package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ValidateSuccessfulAuthCodeFlowResponseFromAuthorizationEndpoint extends AbstractValidateSuccessfulResponseFromAuthorizationEndpoint {

	@Override
	protected List<String> getExpectedParams()
	{
		// dummy1/2 are returned in the url query for the second client as we require users to register a redirect
		// url that contains an existing url query
		return ImmutableList.of("code", "state", "id_token", "session_state", "iss", "dummy1", "dummy2");
	}

}
