package net.openid.conformance.openbanking_brasil.testmodules.account;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openBanking/swagger_accounts_apis-v2.yaml
 * Api endpoint: /accounts/{accountId}
 * Api version: 2.0.0.final
 **/

@ApiName("Account Identification V2")
public class AccountIdentificationResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);

	public static final Set<String> ENUM_TYPE = SetUtils.createSet("CONTA_DEPOSITO_A_VISTA, CONTA_POUPANCA, CONTA_PAGAMENTO_PRE_PAGA");
	public static final Set<String> ENUM_SUB_TYPE = SetUtils.createSet("INDIVIDUAL, CONJUNTA_SIMPLES, CONJUNTA_SOLIDARIA");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder("data")
				.setValidator(this::assertData)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonElement data) {
		assertField(data,
			new StringField
				.Builder("compeCode")
				.setPattern("^\\d{3}$")
				.setMaxLength(3)
				.build());

		assertField(data,
			new StringField
				.Builder("branchCode")
				.setPattern("^\\d{4}$")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("number")
				.setPattern("^\\d{8,20}$")
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
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());
	}
}
