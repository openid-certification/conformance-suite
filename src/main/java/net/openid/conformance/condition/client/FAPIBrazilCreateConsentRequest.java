package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.models.external.OBBrasilConsentPermissions;
import net.openid.conformance.models.external.OpenBankingBrasilConsentRequest;
import net.openid.conformance.testmodule.Environment;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class FAPIBrazilCreateConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		String cpf = env.getString("config", "resource.brazilCpf");
		String cnpj = env.getString("config", "resource.brazilCnpj");
		if (Strings.isNullOrEmpty(cpf) && Strings.isNullOrEmpty(cnpj)) {
			throw error("A least one of CPF and CNPJ must be specified in the test configuration");
		}

		String[] permissions = Optional.ofNullable(env.getString("consent_permissions"))
			.map(c -> c.split("\\W"))
			.orElse(new String[] {"ACCOUNTS_READ"});

		OpenBankingBrasilConsentRequest consentRequest =
			new OpenBankingBrasilConsentRequest(cpf, cnpj, permissions);
		JsonObject requestObject = consentRequest.toJson();

		env.putObject("consent_endpoint_request", requestObject);

		logSuccess(args("consent_endpoint_request", requestObject));

		return env;
	}

}
