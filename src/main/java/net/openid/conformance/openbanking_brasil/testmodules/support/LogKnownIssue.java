package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class LogKnownIssue extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env){
		logSuccess("The current block contains a known issue that can be viewed on the link");
		return env;
	}
}
