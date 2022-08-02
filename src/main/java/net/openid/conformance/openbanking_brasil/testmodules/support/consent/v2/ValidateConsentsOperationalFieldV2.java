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

		String productType = env.getString("config", "consent.productType");
		if (Strings.isNullOrEmpty(productType)) {
			throw error("Product type (Business or Personal) must be specified in the test configuration");
		}

		JsonElement brazilCpfOperationalElement;
		String brazilCpfOperational;
		JsonElement brazilCnpjOperationalElement;
		String brazilCnpjOperational;

		if (productType.equals("business")) {
			brazilCpfOperationalElement = findElementOrThrowError(config, "$.resource.brazilCpfOperationalBusiness");
			brazilCpfOperational = OIDFJSON.getString(brazilCpfOperationalElement);
			if(Strings.isNullOrEmpty(brazilCpfOperational)) {
				logFailure("brazilCpfOperationalBusiness is not valid", args("brazilCpfOperational", brazilCpfOperational));
			}

			brazilCnpjOperationalElement = findElementOrThrowError(config, "$.resource.brazilCnpjOperationalBusiness");
			brazilCnpjOperational = OIDFJSON.getString(brazilCnpjOperationalElement);
			if(Strings.isNullOrEmpty(brazilCnpjOperational)) {
				logFailure("brazilCnpjOperationalBusiness is not valid", args("brazilCpfOperational", brazilCpfOperational));
			}
		} else {
			brazilCpfOperationalElement = findElementOrThrowError(config, "$.resource.brazilCpfOperationalPersonal");
			brazilCpfOperational = OIDFJSON.getString(brazilCpfOperationalElement);
			if(Strings.isNullOrEmpty(brazilCpfOperational)) {
				logFailure("brazilCpfOperationalPersonal is not valid", args("brazilCpfOperational", brazilCpfOperational));
			}
		}

		JsonElement clientIdOperationalElement = findElementOrThrowError(config, "$.client.client_id_operational_limits");
		String clientIdOperational = OIDFJSON.getString(clientIdOperationalElement);
		if (Strings.isNullOrEmpty(clientIdOperational)) {
			logFailure("client ID for Operational Limits not found");
		}

        return super.evaluate(env);
	}
}
