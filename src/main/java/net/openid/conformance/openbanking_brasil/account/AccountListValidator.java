package net.openid.conformance.openbanking_brasil.account;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.fields.StringField;

import java.util.Set;

/**
 * This is validator for API-Contas|Lista de contas
 * https://openbanking-brasil.github.io/areadesenvolvedor/#lista-de-contas
 */
@ApiName("Accounts list")
public class AccountListValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {

		Set<String> enumType = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA");

		assertStringField(body,
			new StringField
				.Builder("brandName")
				.setPattern(".+")
				.setMaxLength(80)
				.build());

		assertStringField(body,
			new StringField
				.Builder("companyCnpj")
				.setPattern("\\d{14}|^NA$")
				.setMaxLength(14)
				.build());

		assertStringField(body,
			new StringField
				.Builder("type")
				.setEnums(enumType)
				.build());

		assertStringField(body,
			new StringField
				.Builder("compeCode")
				.setPattern("\\d{3}|^NA$")
				.setMaxLength(3)
				.build());

		assertStringField(body,
			new StringField
				.Builder("branchCode")
				.setPattern("\\d{4}|^NA$")
				.setMaxLength(4)
				.build());

		assertStringField(body,
			new StringField
				.Builder("number")
				.setPattern("^\\d{8,20}$|^NA$")
				.setMaxLength(20)
				.build());

		assertStringField(body,
			new StringField
				.Builder("checkDigit")
				.setPattern("\\w*\\W*")
				.setMaxLength(1)
				.build());

		assertStringField(body,
			new StringField
				.Builder("accountId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.build());


	}
}
