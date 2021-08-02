package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class CheckErrorFromDynamicRegistrationEndpointIsInvalidSoftwareStatement extends AbstractCheckErrorFromDynamicRegistrationEndpoint {

	@Override
	List<String> getPermittedErrors() {
		return ImmutableList.of("invalid_software_statement");
	}

}
