package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

/**
 * @author jricher
 *
 */
public class AuthorizationEndpointRequestCodeIdToken extends AbstractConditionSequence {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.sequence.ConditionSequence#evaluate()
	 */
	@Override
	public void evaluate() {

		call(condition(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class));

	}

}
