package net.openid.conformance.condition.common;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class CheckCIBAServerConfiguration extends AbstractCheckServerConfiguration {

	@Override
	protected List<String> getExpectedListEndpoint() {
		return ImmutableList.of(
			"backchannel_authentication_endpoint",
			"token_endpoint",
			"issuer");
	}

}
