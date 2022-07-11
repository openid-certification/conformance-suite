package net.openid.conformance.openbanking_brasil.testmodules.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.testmodules.support.LinksAndMetaValidatorTransactions;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: swagger/openBanking/swagger_accounts_apis-v2.yaml
 * Api endpoint: /accounts/{accountId}/transactions-current
 * Api version: 2.0.0.final
 **/
@ApiName("Account Transactions Current V2")
public class AccountTransactionsCurrentValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidatorTransactions linksAndMetaValidator = new LinksAndMetaValidatorTransactions(this);

	public static final Set<String> ENUM_COMPLETED_AUTHORISED_PAYMENT_INDICATOR = SetUtils.createSet("TRANSACAO_EFETIVADA, LANCAMENTO_FUTURO");
	public static final Set<String> ENUM_TRANSACTION_TYPES = SetUtils.createSet("TED, DOC, PIX, TRANSFERENCIA_MESMA_INSTITUICAO, BOLETO, CONVENIO_ARRECADACAO, PACOTE_TARIFA_SERVICOS, TARIFA_SERVICOS_AVULSOS, FOLHA_PAGAMENTO, DEPOSITO, SAQUE, CARTAO, ENCARGOS_JUROS_CHEQUE_ESPECIAL, RENDIMENTO_APLIC_FINANCEIRA, PORTABILIDADE_SALARIO, RESGATE_APLIC_FINANCEIRA, OPERACAO_CREDITO, OUTROS");
	public static final Set<String> ENUM_CREDIT_DEBIT_INDICATOR = SetUtils.createSet("CREDITO, DEBITO");
	public static final Set<String> ENUM_PARTIE_PERSON_TYPE = SetUtils.createSet("PESSOA_NATURAL, PESSOA_JURIDICA");

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
				.Builder("transactionId")
				.setOptional()
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.build());

		assertField(body,
			new StringField
				.Builder("completedAuthorisedPaymentType")
				.setEnums(ENUM_COMPLETED_AUTHORISED_PAYMENT_INDICATOR)
				.build());

		assertField(body,
			new StringField
				.Builder("creditDebitType")
				.setEnums(ENUM_CREDIT_DEBIT_INDICATOR)
				.build());

		assertField(body,
			new StringField
				.Builder("transactionName")
				.setMaxLength(60)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setEnums(ENUM_TRANSACTION_TYPES)
				.build());

		assertField(body,
			new ObjectField
				.Builder("transactionAmount")
				.setValidator(this::assertAmount)
				.build());

		assertField(body,
			new StringField
				.Builder("transactionDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(body,
			new StringField
				.Builder("partieCnpjCpf")
				.setMaxLength(14)
				.setPattern("^\\d{11}$|^\\d{14}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("partiePersonType")
				.setEnums(ENUM_PARTIE_PERSON_TYPE)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("partieCompeCode")
				.setMaxLength(3)
				.setPattern("^\\d{3}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("partieBranchCode")
				.setMaxLength(4)
				.setPattern("^\\d{4}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("partieNumber")
				.setMaxLength(20)
				.setPattern("^\\d{8,20}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("partieCheckDigit")
				.setMaxLength(1)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
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
