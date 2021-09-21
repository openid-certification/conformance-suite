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
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_credit_cards_apis.yaml
 * Api endpoint: /accounts/{creditCardAccountId}/bills
 * Api git hash: 91e2ff8327cb35eb1ae571c7b2264e6173b34eeb
 */
@ApiName("Credit Card Bill")
public class CreditCardBillValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);

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

		assertField(data,
			new ObjectArrayField
				.Builder("financeCharges")
				.setValidator(this::assertInnerFieldsFinanceCharges)
				.setMinItems(1)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("payments")
				.setValidator(this::assertInnerFieldsPayments)
				.setMinItems(0)
				.build());
	}

	private void assertInnerFieldsFinanceCharges(JsonObject data) {
		Set<String> enumType = Sets.newHashSet("JUROS_REMUNERATORIOS_ATRASO_PAGAMENTO_FATURA",
			"MULTA_ATRASO_PAGAMENTO_FATURA", "JUROS_MORA_ATRASO_PAGAMENTO_FATURA",
			"IOF", "SEM_ENCARGO", "OUTROS");

		assertField(data, new StringField
			.Builder("type")
			.setMaxLength(44)
			.setEnums(enumType)
			.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setMaxLength(20)
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setNullable()
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
