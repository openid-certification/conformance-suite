package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API - Cartão de Crédito | Fatura de Cartão de Crédito
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#fatura-de-cartao-de-credito">Fatura de Cartão de Crédito</a>
 **/
@ApiName("Credit Card Bill")
public class CreditCardBillValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);
		assertHasField(body, "$.links");
		assertField(body, new StringField.Builder("$.links.self").setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.first").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.prev").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.next").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.last").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		return environment;
	}

	private void assertInnerFields(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("billId")
				.setMaxLength(100)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.build());

		assertField(data,
			new StringField
				.Builder("dueDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(data,
			new DoubleField
				.Builder("billTotalAmount")
				.setMaxLength(20)
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("billTotalAmountCurrency")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.build());

		assertField(data,
			new DoubleField
				.Builder("billMinimumAmount")
				.setMaxLength(19)
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("billMinimumAmountCurrency")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.build());

		assertField(data,
			new BooleanField
				.Builder("isInstalment")
				.build());

		assertFinanceCharges(data);
		assertPayments(data);
	}

	private void assertPayments(JsonObject data) {
		assertField(data,
			new ArrayField
				.Builder("payments")
				.build());

		assertJsonArrays(data, "payments", this::assertInnerFieldsPayments);
	}

	private void assertFinanceCharges(JsonObject data) {
		assertField(data,
			new ArrayField
				.Builder("financeCharges")
				.setMinItems(1)
				.build());

		assertJsonArrays(data, "financeCharges", this::assertInnerFieldsFinanceCharges);
	}

	private void assertInnerFieldsFinanceCharges(JsonObject data) {
		Set<String> enumType = Sets.newHashSet("JUROS_REMUNERATORIOS_ATRASO_PAGAMENTO_FATURA",
			"MULTA_ATRASO_PAGAMENTO_FATURA", "JUROS_MORA_ATRASO_PAGAMENTO_FATURA",
			"IOF", "SEM_ENCARGO", "OUTROS");

		assertField(data, new StringField
			.Builder("type")
			.setEnums(enumType)
			.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(140)
				//.setPattern("\\w*\\W*")//TODO wrong pattern
				.setOptional()
				.build());

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setMaxLength(20)
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setMaxLength(3)
				.setPattern("(\\w{3}){1}$")
				.build());
	}

	private void assertInnerFieldsPayments(JsonObject data) {
		Set<String> valueType = Sets.newHashSet("VALOR_PAGAMENTO_FATURA_PARCELADO",
			"VALOR_PAGAMENTO_FATURA_REALIZADO", "OUTRO_VALOR_PAGO_FATURA");
		Set<String> paymentMode = Sets.newHashSet("DEBITO_CONTA_CORRENTE",
			"BOLETO_BANCARIO", "AVERBACAO_FOLHA", "PIX");

		assertField(data,
			new StringField
				.Builder("valueType")
				.setEnums(valueType)
				.build());

		assertField(data,
			new StringField
				.Builder("paymentDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(data,
			new StringField
				.Builder("paymentMode")
				.setEnums(paymentMode)
				.build());

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setMaxLength(20)
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
				.build());
	}
}
