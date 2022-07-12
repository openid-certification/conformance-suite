package net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.CpfCnpjValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.JsonHelper;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.DatetimeField;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ValidateConsentsFieldV2 extends AbstractJsonAssertingCondition {

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
		if(!CpfCnpjValidator.isValidCPF(brazilCpf)) {
			logFailure("brazilCpf is not valid");
		}

		JsonElement brazilCpfOperationalElement = findElementOrThrowError(config, "$.resource.brazilCpfOperational");
		String brazilCpfOperational = OIDFJSON.getString(brazilCpfOperationalElement);
		if(!CpfCnpjValidator.isValidCPF(brazilCpfOperational)) {
			logFailure("brazilCpfOperational is not valid");
		}

		JsonElement productTypeElement = findElementOrThrowError(config, "$.consent.productType");
		String productType = OIDFJSON.getString(productTypeElement);
		if (Strings.isNullOrEmpty(productType)) {
			logFailure("Product type (Business or Personal) must be specified in the test configuration");
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
