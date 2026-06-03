package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetConnectIdBindingMessageToPurpose extends AbstractCondition {

	public static final String CONNECTID_PURPOSE = "Confirm sharing your identity details with the relying party";

	@Override
	@PostEnvironment(strings = "requested_binding_message")
	public Environment evaluate(Environment env) {
		env.putString("requested_binding_message", CONNECTID_PURPOSE);

		logSuccess("Set ConnectID binding_message purpose", args("binding_message", CONNECTID_PURPOSE));

		return env;
	}
}
