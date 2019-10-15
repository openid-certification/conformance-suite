package net.openid.conformance.condition.common;

import java.util.List;
import com.google.common.collect.ImmutableList;

public class CheckHeartServerConfiguration extends AbstractCheckServerConfiguration {

	@Override
	protected List<String> getExpectedListEndpoint() {
		return ImmutableList.of("authorization_endpoint",
			"token_endpoint",
			"issuer",
			"introspection_endpoint",
			"revocation_endpoint",
			"jwks_uri");
	}
}
