package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class LoadServerAndClientConfiguration extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		call(condition(CreateRedirectUri.class));

		call(exec().exposeEnvironmentString("redirect_uri"));

		call(condition(GetDynamicServerConfiguration.class).dontStopOnFailure().onFail(Condition.ConditionResult.WARNING));

		call(condition(GetStaticServerConfiguration.class).dontStopOnFailure().onFail(Condition.ConditionResult.WARNING));

		call(condition(CheckServerConfiguration.class));

		call(condition(ExtractTLSTestValuesFromServerConfiguration.class));

		call(condition(FetchServerKeys.class));

		// Dynamic is using dynamic registration which is a whole nother thing.
		//call(condition(GetDynamicClientConfiguration.class).dontStopOnFailure());

		runAccessory("client_configuration",
			condition(GetStaticClientConfiguration.class)); //.dontStopOnFailure());

		call(exec().exposeEnvironmentString("client_id"));
	}
}
