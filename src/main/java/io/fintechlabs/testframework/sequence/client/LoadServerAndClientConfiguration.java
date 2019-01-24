package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.client.*;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

import javax.persistence.criteria.Fetch;

public class LoadServerAndClientConfiguration extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		call(condition(CreateRedirectUri.class));

		call(exec().exposeEnvironmentString("redirect_uri"));

		call(condition(GetDynamicServerConfiguration.class).dontStopOnFailure());

		call(condition(GetStaticServerConfiguration.class).dontStopOnFailure());

		call(condition(CheckServerConfiguration.class));

		call(condition(ExtractTLSTestValuesFromServerConfiguration.class));

		call(condition(FetchServerKeys.class));

		// Dynamic is using dynamic registration which is a whole nother thing.
		//call(condition(GetDynamicClientConfiguration.class).dontStopOnFailure());

		if (hasAccessory("client_configuration")) {
			call(getAccessories("client_configuration"));
		} else {
			call(condition(GetStaticClientConfiguration.class)); //.dontStopOnFailure());
		}
		call(exec().exposeEnvironmentString("client_id"));
	}
}
