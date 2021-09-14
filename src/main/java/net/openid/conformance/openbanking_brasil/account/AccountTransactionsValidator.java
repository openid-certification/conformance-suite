package net.openid.conformance.openbanking_brasil.account;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.Field;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API-Contas| Transações da contas
 * https://openbanking-brasil.github.io/areadesenvolvedor/#transacoes-da-conta
 */
@ApiName("Account Transactions")
public class AccountTransactionsValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ArrayField.Builder("data")
				.setMinItems(1)
				.build());
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);
		assertHasField(body, "$.links");
		assertField(body, new StringField.Builder("$.links.self").setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.first").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.prev").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.next").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.last").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		Set<String> enumCompletedAuthorisedPaymentIndicator = Sets.newHashSet("TRANSACAO_EFETIVADA", "LANCAMENTO_FUTURO");
		Set<String> enumCreditDebitIndicator = Sets.newHashSet("CREDITO", "DEBITO");
		Set<String> enumTransactionTypes = Sets.newHashSet(
			"TED", "DOC", "PIX", "TRANSFERENCIA_MESMA_INSTITUICAO",
			"BOLETO", "CONVENIO_ARRECADACAO", "PACOTE_TARIFA_SERVICOS",
			"TARIFA_SERVICOS_AVULSOS", "FOLHA_PAGAMENTO", "DEPOSITO",
			"SAQUE", "CARTAO", "ENCARGOS_JUROS_CHEQUE_ESPECIAL",
			"RENDIMENTO_APLIC_FINANCEIRA", "PORTABILIDADE_SALARIO",
			"RESGATE_APLIC_FINANCEIRA", "OPERACAO_CREDITO", "OUTROS");
		Set<String> enumPartiePersonType = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA");

		assertField(body,
			new StringField
				.Builder("transactionId")
				.setOptional()
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.build());

		assertField(body,
			new StringField
				.Builder("completedAuthorisedPaymentType")
				.setEnums(enumCompletedAuthorisedPaymentIndicator)
				.build());

		assertField(body,
			new StringField
				.Builder("creditDebitType")
				.setMaxLength(7)
				.setEnums(enumCreditDebitIndicator)
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
				.setMaxLength(31)
				.setEnums(enumTransactionTypes)
				.build());

		assertField(body,
			new DoubleField
				.Builder("amount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(0)
				.build());

		assertField(body,
			new StringField
				.Builder("transactionCurrency")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
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
				.setPattern("^\\d{11}$|^\\d{14}$|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("partiePersonType")
				.setEnums(enumPartiePersonType)
				.build());

		assertField(body,
			new StringField
				.Builder("partieCompeCode")
				.setMaxLength(3)
				.setPattern("\\d{3}|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("partieBranchCode")
				.setMaxLength(4)
				.setPattern("\\d{4}|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("partieNumber")
				.setMaxLength(20)
				.setPattern("^\\d{8,20}$|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("partieCheckDigit")
				.setMaxLength(1)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}
}
