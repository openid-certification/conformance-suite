package net.openid.conformance.fapiciba.rp;

import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.common.AbstractCheckServerConfiguration;

import java.util.List;

public class CheckServerConfiguration extends AbstractCheckServerConfiguration {

	@Override
	protected List<String> getExpectedListEndpoint() {
		return ImmutableList.of("backchannel_authentication_endpoint", "token_endpoint", "issuer");
	}

}
