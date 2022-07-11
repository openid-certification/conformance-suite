package net.openid.conformance.openbanking_brasil.testmodules.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openBanking/swagger_accounts_apis-v2.yaml
 * Api endpoint: /accounts
 * Api version: 2.0.0.final
 **/
@ApiName("Accounts list V2")
public class AccountListValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);

	public static final Set<String> ENUM_TYPE = SetUtils.createSet("CONTA_DEPOSITO_A_VISTA, CONTA_POUPANCA, CONTA_PAGAMENTO_PRE_PAGA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertData)
				.setMinItems(0)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("brandName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(80)
				.build());

		assertField(body,
			new StringField
				.Builder("companyCnpj")
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(ENUM_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("compeCode")
				.setPattern("^\\d{3}$")
				.setMaxLength(3)
				.build());

		assertField(body,
			new StringField
				.Builder("branchCode")
				.setPattern("^\\d{4}$")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("number")
				.setPattern("^\\d{8,20}$")
				.setMaxLength(20)
				.build());

		assertField(body,
			new StringField
				.Builder("checkDigit")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(1)
				.build());

		assertField(body,
			new StringField
				.Builder("accountId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.build());
	}
}
