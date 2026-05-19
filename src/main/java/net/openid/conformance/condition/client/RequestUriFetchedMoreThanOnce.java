package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RequestUriFetchedMoreThanOnce extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Wallet fetched request_uri more than once. There is no reason a wallet should fetch this multiple times, and verifiers may not support it being retrieved more than once.");
	}

}
