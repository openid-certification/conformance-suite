package net.openid.conformance.authzen.condition;

import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.common.AbstractCheckServerConfiguration;

import java.util.List;

public class CheckPDPServerConfiguration extends AbstractCheckServerConfiguration {

	@Override
	protected List<String> getExpectedListEndpoint() {
		return ImmutableList.of("policy_decision_point",
			"access_evaluation_endpoint");
	}

}
