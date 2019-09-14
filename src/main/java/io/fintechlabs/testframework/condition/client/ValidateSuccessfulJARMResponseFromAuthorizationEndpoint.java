package io.fintechlabs.testframework.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class ValidateSuccessfulJARMResponseFromAuthorizationEndpoint extends AbstractValidateSuccessfulResponseFromAuthorizationEndpoint {

	@Override
	protected List<String> getExpectedParams()
	{
		return ImmutableList.of("code", "state", "session_state");
	};

}
