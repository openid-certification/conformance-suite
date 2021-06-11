package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

public class FAPIBrazilCreateConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment()
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		String cpf = env.getString("config", "resource.brazilCpf");
		if (Strings.isNullOrEmpty(cpf)) {
			throw error("CPF value missing from test configuration");
		}

		// see https://openbanking-brasil.github.io/areadesenvolvedor/#direitos-creditorios-descontados-parcelas-do-contrato

		String json =
			"{\n" +
			"  \"data\": {\n" +
			"    \"permissions\": [\n" +
			"      \"ACCOUNTS_READ\"\n" +
			"    ],\n" +
			"    \"loggedUser\": {\n" +
			"      \"document\": {\n" +
			"        \"identification\": \""+cpf+"\",\n" +
			"        \"rel\": \"CPF\"\n" +
			"      }\n" +
			"    }\n" +
			"  }\n" +
			"}";
		JsonObject o = (JsonObject) new JsonParser().parse(json);
		env.putObject("consent_endpoint_request", o);

		logSuccess(args("consent_endpoint_request", o));

		return env;
	}

}
