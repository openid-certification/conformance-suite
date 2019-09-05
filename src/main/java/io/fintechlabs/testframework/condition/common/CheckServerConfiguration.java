package io.fintechlabs.testframework.condition.common;

import com.google.common.collect.ImmutableList;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class CheckServerConfiguration extends AbstractCheckServerConfiguration {

	@Override
	protected List<String> getExpectedListEndpoint() {
		return ImmutableList.of("authorization_endpoint",
			"token_endpoint",
			"issuer");
	}

	@Override
	protected void ensureUrl(Environment in, String path) {}
}
