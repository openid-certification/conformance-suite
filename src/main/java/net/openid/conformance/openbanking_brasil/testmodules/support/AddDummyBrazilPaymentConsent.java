package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddDummyBrazilPaymentConsent extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {

		String brazilPaymentConsentString = "\n" +
			"{\n" +
			"  \"data\": {\n" +
			"    \"loggedUser\": {\n" +
			"      \"document\": {\n" +
			"        \"identification\": \"11111111111\",\n" +
			"        \"rel\": \"CPF\"\n" +
			"      }\n" +
			"    },\n" +
			"    \"creditor\": {\n" +
			"      \"name\": \"Pessoa Inexistente\",\n" +
			"      \"cpfCnpj\": \"11111111111\",\n" +
			"      \"personType\": \"PESSOA_NATURAL\"\n" +
			"    },\n" +
			"    \"payment\": {\n" +
			"      \"date\": \"YYYY-MM-DD\",\n" +
			"      \"amount\": \"0.01\",\n" +
			"      \"currency\": \"BRL\",\n" +
			"      \"details\": {\n" +
			"        \"proxy\": \"11111111111\",\n" +
			"        \"localInstrument\": \"DICT\",\n" +
			"        \"creditorAccount\": {\n" +
			"          \"number\": \"0012345678\",\n" +
			"          \"accountType\": \"CACC\",\n" +
			"          \"ispb\": \"99999008\",\n" +
			"          \"issuer\": \"0001\"\n" +
			"        }\n" +
			"      }\n" +
			"    }\n" +
			"  }\n" +
			"}";

		JsonObject brazilPaymentConsentObject = JsonParser.parseString(brazilPaymentConsentString).getAsJsonObject();
		env.putObject("config", "resource.brazilPaymentConsent", brazilPaymentConsentObject);
		logSuccess("Dummy brazilPaymentConsent added successfully", brazilPaymentConsentObject);
		return env;
	}
}
