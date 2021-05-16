package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ValidateSuccessfulHybridResponseFromAuthorizationEndpoint extends AbstractValidateSuccessfulResponseFromAuthorizationEndpoint {

	@Override
	protected List<String> getExpectedParams()
	{
		return ImmutableList.of("code", "state", "id_token", "session_state", "iss");
	}

}
