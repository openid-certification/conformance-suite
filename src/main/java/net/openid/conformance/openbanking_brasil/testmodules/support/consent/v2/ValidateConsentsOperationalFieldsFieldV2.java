package net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.JsonHelper;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateConsentsOperationalFieldsFieldV2 extends ValidateConsentsFieldV2 {
    @Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement config = env.getObject("config");
		validateMainFields(config);

		JsonElement productTypeElement = findElementOrThrowError(config, "$.consent.productType");
		String productType = OIDFJSON.getString(productTypeElement);
		if (Strings.isNullOrEmpty(productType)) {
			logFailure("Product type (Business or Personal) must be specified in the test configuration");
		}

		if(productType.equals("business")) {
			JsonElement brazilCnpjElement = findElementOrThrowError(config, "$.resource.brazilCnpj");
			String brazilCnpj = OIDFJSON.getString(brazilCnpjElement);
			if(Strings.isNullOrEmpty(brazilCnpj) || brazilCnpj.length() != super.cnpjLength) {
				logFailure("brazilCnpj is not valid", args("brazilCnpj", brazilCnpj));
			}
		}

		//Extra checks for Operational Limits tests
		JsonElement brazilCpfOperationalElement = findElementOrThrowError(config, "$.resource.brazilCpfOperational");
		String brazilCpfOperational = OIDFJSON.getString(brazilCpfOperationalElement);
		if(Strings.isNullOrEmpty(brazilCpfOperational)) {
			logFailure("brazilCpfOperational is not valid", args("brazilCpfOperational", brazilCpfOperational));
		}

		JsonElement clientIdOperationalElement = findElementOrThrowError(config, "$.client.client_id_operational_limits");
		String clientIdOperational = OIDFJSON.getString(clientIdOperationalElement);
		if (Strings.isNullOrEmpty(clientIdOperational)) {
			logFailure("client ID for Operational Limits not found");
		}

        return env;
	}
}
