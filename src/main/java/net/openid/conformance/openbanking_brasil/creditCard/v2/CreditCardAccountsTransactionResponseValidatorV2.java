package net.openid.conformance.openbanking_brasil.creditCard.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.creditCard.LinksAndMetaValidatorTransactions;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api: swagger/openBanking/swagger-credit-cards-api-V2.yaml
 * Api endpoint: /accounts/{creditCardAccountId}/transactions
 * Api version: 2.0.1.final
 **/

@ApiName("Credit Card Accounts Transaction V2")
public class CreditCardAccountsTransactionResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidatorTransactions linksAndMetaValidator = new LinksAndMetaValidatorTransactions(this);
	private static final Set<String> ENUM_PAYMENT_TYPE = SetUtils.createSet("A_VISTA, A_PRAZO");
	public static final Set<String> ENUM_CREDIT_DEBIT_TYPE = SetUtils.createSet("CREDITO, DEBITO");
	private static final Set<String> ENUM_FEE_TYPE = SetUtils.createSet("ANUIDADE, SAQUE_CARTAO_BRASIL, SAQUE_CARTAO_EXTERIOR, AVALIACAO_EMERGENCIAL_CREDITO, EMISSAO_SEGUNDA_VIA, TARIFA_PAGAMENTO_CONTAS, SMS, OUTRA");
	private static final Set<String> ENUM_TRANSACTION_TYPE = SetUtils.createSet("PAGAMENTO, TARIFA, OPERACOES_CREDITO_CONTRATADAS_CARTAO, ESTORNO, CASHBACK, OUTROS");
	private static final Set<String> ENUM_CREDITS_TYPE = SetUtils.createSet("CREDITO_ROTATIVO, PARCELAMENTO_FATURA, EMPRESTIMO, OUTROS");

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

	private void assertData(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("transactionId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("identificationNumber")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.build());

		assertField(data,
			new StringField
				.Builder("transactionName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.build());

		assertField(data,
			new StringField
				.Builder("billId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("creditDebitType")
				.setEnums(ENUM_CREDIT_DEBIT_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("transactionType")
				.setEnums(ENUM_TRANSACTION_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("transactionalAdditionalInfo")
				.setMaxLength(140)
				.setPattern("^\\S[\\s\\S]*$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("paymentType")
				.setEnums(ENUM_PAYMENT_TYPE)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("feeType")
				.setOptional()
				.setEnums(ENUM_FEE_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("feeTypeAdditionalInfo")
				.setMaxLength(140)
				.setPattern("^\\S[\\s\\S]*$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("otherCreditsType")
				.setEnums(ENUM_CREDITS_TYPE)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("otherCreditsAdditionalInfo")
				.setMaxLength(50)
				.setPattern("^\\S[\\s\\S]*$")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("chargeIdentificator")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new IntField
				.Builder("chargeNumber")
				.setMaxValue(999)
				.setOptional()
				.build());

		assertField(data,
			new ObjectField
				.Builder("brazilianAmount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new StringField
				.Builder("transactionDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(20)
				.build());

		assertField(data,
			new StringField
				.Builder("billPostDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(20)
				.build());

		assertField(data,
			new IntField
				.Builder("payeeMCC")
				.setOptional()
				.setMaxValue(2147483647)
				.build());
	}

	private void assertAmount(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(20)
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.build());
	}
}
