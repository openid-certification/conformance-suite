package net.openid.conformance.openbanking_brasil.account;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API-Contas "Identificação da Conta"
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#identificacao-da-conta">Identificação da Conta</a>
 **/

@ApiName("Account Identification")
public class AccountIdentificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		Set<String> enumType = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA");
		Set<String> enumSubType = Sets.newHashSet("INDIVIDUAL", "CONJUNTA_SIMPLES", "CONJUNTA_SOLIDARIA");
		JsonObject data = findByPath(body, "$.data").getAsJsonObject();

		assertField(data,
			new StringField
				.Builder("compeCode")
				.setPattern("\\d{3}|^NA$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new StringField
				.Builder("branchCode")
				.setPattern("\\d{4}|^NA$")
				.setMaxLength(4)
				.build());

		assertField(data,
			new StringField
				.Builder("number")
				.setPattern("^\\d{8,20}$|^NA$")
				.setMaxLength(20)
				.build());

		assertField(data,
			new StringField
				.Builder("checkDigit")
				.setPattern("\\w*\\W*")
				.setMaxLength(1)
				.build());

		assertField(data,
			new StringField
				.Builder("type")
				.setEnums(enumType)
				.build());

		assertField(data,
			new StringField
				.Builder("subtype")
				.setEnums(enumSubType)
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());
	}
}
