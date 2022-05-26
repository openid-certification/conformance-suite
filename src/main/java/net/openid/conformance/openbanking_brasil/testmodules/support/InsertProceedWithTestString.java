package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class InsertProceedWithTestString extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		if(env.getString("proceed_with_test") != null) {
			logSuccess("proceed_with_test field already exists, nothing to be done");
			return env;
		}
		env.putString("proceed_with_test", "true");
		logSuccess("proceed_with_test field added successfully");
		return env;

	}

}
