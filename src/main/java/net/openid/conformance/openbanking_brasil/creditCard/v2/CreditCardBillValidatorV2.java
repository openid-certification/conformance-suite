package net.openid.conformance.openbanking_brasil.creditCard.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api: swagger/openBanking/swagger-credit-cards-api-V2.yaml
 * Api endpoint: /accounts/{creditCardAccountId}/bills
 * Api version: 2.0.0.final
 **/
@ApiName("Credit Card Bill V2")
public class CreditCardBillValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> ENUM_TYPE = SetUtils.createSet("JUROS_REMUNERATORIOS_ATRASO_PAGAMENTO_FATURA, MULTA_ATRASO_PAGAMENTO_FATURA, JUROS_MORA_ATRASO_PAGAMENTO_FATURA, IOF, OUTROS");
	public static final Set<String> VALUE_TYPE = SetUtils.createSet("VALOR_PAGAMENTO_FATURA_PARCELADO, VALOR_PAGAMENTO_FATURA_REALIZADO, OUTRO_VALOR_PAGO_FATURA");
	public static final Set<String> PAYMENT_MODE = SetUtils.createSet("DEBITO_CONTA_CORRENTE, BOLETO_BANCARIO, AVERBACAO_FOLHA, PIX");

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
				.Builder("billId")
				.setMaxLength(100)
				.setMinLength(1)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.build());

		assertField(data,
			new StringField
				.Builder("dueDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(data,
			new ObjectField
				.Builder("billTotalAmount")
				.setValidator(this::assertAmount)
				.build());

		assertField(data,
			new ObjectField
				.Builder("billMinimumAmount")
				.setValidator(this::assertAmount)
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
				.setOptional()
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("payments")
				.setValidator(this::assertInnerFieldsPayments)
				.setMinItems(0)
				.build());
	}

	private void assertInnerFieldsFinanceCharges(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("type")
				.setEnums(ENUM_TYPE)
				.build());

		assertField(data,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("amount")
				.setMaxLength(21)
				.setMinLength(4)
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
		assertField(data,
			new StringField
				.Builder("valueType")
				.setEnums(VALUE_TYPE)
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
				.setEnums(PAYMENT_MODE)
				.build());

		assertField(data,
			new StringField
				.Builder("amount")
				.setMaxLength(20)
				.setMinLength(4)
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$")
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