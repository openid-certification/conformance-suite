package net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateConsentsOperationalFieldV2 extends ValidateConsentsFieldV2 {
    @Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement config = env.getObject("config");
		//Extra checks for Operational Limits tests
		JsonElement brazilCpfOperationalElement = findElementOrThrowError(config, "$.resource.brazilCpfOperational");
		String brazilCpfOperational = OIDFJSON.getString(brazilCpfOperationalElement);
		if(Strings.isNullOrEmpty(brazilCpfOperational)) {
			logFailure("brazilCpfOperational is not valid", args("brazilCpfOperational", brazilCpfOperational));
		}

        return super.evaluate(env);
	}
}
