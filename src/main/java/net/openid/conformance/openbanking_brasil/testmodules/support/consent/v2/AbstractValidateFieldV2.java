package net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.JsonHelper;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AbstractValidateFieldV2 extends AbstractJsonAssertingCondition {
	protected int cpfLength = 11;
	protected int cnpjLength = 14;
    @Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		return env;
	}

	//Fields present across all Phase 2 V2 test plans
	protected void validateMainFields(JsonElement config) {

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
	}

	protected  JsonElement findElementOrThrowError(JsonElement rootElement, String path) {
		if(!JsonHelper.ifExists(rootElement, path)) {
			throw error(String.format("Element with path %s was not found.", path));
		}
		return findByPath(rootElement, path);
	}
}
