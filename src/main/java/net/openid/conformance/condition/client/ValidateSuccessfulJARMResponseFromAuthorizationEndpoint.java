package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ValidateSuccessfulJARMResponseFromAuthorizationEndpoint extends AbstractValidateSuccessfulResponseFromAuthorizationEndpoint {

	@Override
	protected List<String> getExpectedParams()
	{
		return ImmutableList.of("code", "state", "session_state", "iss");
	}

}
