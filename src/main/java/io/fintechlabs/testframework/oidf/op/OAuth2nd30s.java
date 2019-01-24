package io.fintechlabs.testframework.oidf.op;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.common.Wait30Seconds;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

/**
 * @author jricher
 *
 */
@PublishTestModule(testName = "Oauth2nd30s",
	displayName = "Use Auth Code twice with 30 second delay",
	configurationFields = {		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.client_secret"})
public class OAuth2nd30s extends OAuth2nd {

	@Override
	protected ConditionSequence createTokenEndpointResponseSequence() {
		return super.createTokenEndpointResponseSequence().then(condition(Wait30Seconds.class));
	}
}
