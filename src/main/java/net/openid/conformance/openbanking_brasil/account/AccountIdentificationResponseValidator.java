package net.openid.conformance.openbanking_brasil.account;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 *  * API: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_accounts_apis.yaml
 *  * URL: /accounts/{accountId}
 *  * Api git hash: f14f533cf29fdcef0a3ad38e2f49e1f31c5ab7b2
 **/

@ApiName("Account Identification")
public class AccountIdentificationResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_TYPE = Sets.newHashSet("CONTA_DEPOSITO_A_VISTA", "CONTA_POUPANCA", "CONTA_PAGAMENTO_PRE_PAGA");
	public static final Set<String> ENUM_SUB_TYPE = Sets.newHashSet("INDIVIDUAL", "CONJUNTA_SIMPLES", "CONJUNTA_SOLIDARIA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);

		return environment;
	}

	private void assertInnerFields(JsonElement body) {
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
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(1)
				.build());

		assertField(data,
			new StringField
				.Builder("type")
				.setEnums(ENUM_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("subtype")
				.setEnums(ENUM_SUB_TYPE)
				.setMaxLength(18)
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());
	}
}
