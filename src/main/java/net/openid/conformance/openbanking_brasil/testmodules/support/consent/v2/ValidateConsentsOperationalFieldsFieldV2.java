package net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.JsonHelper;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateConsentsOperationalFieldsFieldV2 extends AbstractJsonAssertingCondition {
	private int cpfLength = 11;
	private int cnpjLength = 14;
    @Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement config = env.getObject("config");

		JsonElement consentElement = findElementOrThrowError(config, "$.resource.consentUrl");
		String consentUrl = OIDFJSON.getString(consentElement);
		String regexValidator = "^(https://)(.*?)(consents/v2/consents)";
		if(!consentUrl.matches(regexValidator)) {
			logFailure(String.format("consentUrl does not match the regex %s", regexValidator));
		}

		JsonElement brazilCpfElement = findElementOrThrowError(config, "$.resource.brazilCpf");
		String brazilCpf = OIDFJSON.getString(brazilCpfElement);
		if(Strings.isNullOrEmpty(brazilCpf) || brazilCpf.length() != cpfLength) {
			logFailure("brazilCpf is not valid", args("brazilCpf", brazilCpf));
		}

		JsonElement productTypeElement = findElementOrThrowError(config, "$.consent.productType");
		String productType = OIDFJSON.getString(productTypeElement);
		if (Strings.isNullOrEmpty(productType)) {
			logFailure("Product type (Business or Personal) must be specified in the test configuration");
		}

		if(productType.equals("business")) {
			JsonElement brazilCnpjElement = findElementOrThrowError(config, "$.resource.brazilCnpj");
			String brazilCnpj = OIDFJSON.getString(brazilCnpjElement);
			if(Strings.isNullOrEmpty(brazilCnpj) || brazilCnpj.length() != cnpjLength) {
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

	protected  JsonElement findElementOrThrowError(JsonElement rootElement, String path) {
		if(!JsonHelper.ifExists(rootElement, path)) {
			throw error(String.format("Element with path %s was not found.", path));
		}
		return findByPath(rootElement, path);
	}
}
