package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CdrEnsureUnrecognisedArrangementIdRejected extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("The authorisation request contained a cdr_arrangement_id that is not recognised by the Data Holder, so the request must be rejected, but the Data Holder allowed the authorisation to complete");
	}

}
