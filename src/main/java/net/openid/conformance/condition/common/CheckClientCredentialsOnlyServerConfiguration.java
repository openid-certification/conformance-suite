package net.openid.conformance.condition.common;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class CheckClientCredentialsOnlyServerConfiguration extends AbstractCheckServerConfiguration {

	@Override
	protected List<String> getExpectedListEndpoint() {
		return ImmutableList.of(
			"token_endpoint",
			"issuer");
	}

}
