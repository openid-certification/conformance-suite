package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.models.external.OpenBankingBrasilConsentRequest;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

public class FAPIBrazilCreateConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		String cpf = env.getString("config", "resource.brazilCpf");
		String cnpj = env.getString("config", "resource.brazilCnpj");
		if (Strings.isNullOrEmpty(cpf) && Strings.isNullOrEmpty(cnpj)) {
			throw error("A least one of CPF and CNPJ must be specified in the test configuration");
		}

		String[] permissions;

		String consentPermissions = env.getString("consent_permissions");
		if (Strings.isNullOrEmpty(consentPermissions)) {
			String scope = env.getString("client", "scope");
			if (Strings.isNullOrEmpty(scope)) {
				throw error("scope missing from client configuration");
			}
			List<String> scopes = Arrays.asList(scope.split(" "));
			// This is believed to be the most minimal set of permissions that will allow a successful
			// operation at the accounts endpoint. The Brazil user experience guidelines do not allow
			// only ACCOUNTS_READ to be required.
			permissions = new String[]{"ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ", "RESOURCES_READ"};
		} else {
			permissions = consentPermissions.split("\\W");
		}

		OpenBankingBrasilConsentRequest consentRequest =
			new OpenBankingBrasilConsentRequest(cpf, cnpj, permissions);
		JsonObject requestObject = consentRequest.toJson();

		env.putObject("consent_endpoint_request", requestObject);

		logSuccess(args("consent_endpoint_request", requestObject));

		return env;
	}

}
