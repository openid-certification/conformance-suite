package net.openid.conformance.openbanking_brasil.creditOperations.financing.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.creditOperations.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.NumberField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openinsurance/financings/v2/swagger_financings_apis-v2.yaml
 * URL: /contracts/{contractId}/payments
 * Api version: 2.0.0.final
 */

@ApiName("Financing Payments V2")
public class FinancingPaymentsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> ENUM_CHARGE_TYPE = SetUtils.createSet("JUROS_REMUNERATORIOS_POR_ATRASO, MULTA_ATRASO_PAGAMENTO, JUROS_MORA_ATRASO, IOF_CONTRATACAO, IOF_POR_ATRASO, SEM_ENCARGO, OUTROS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectField
				.Builder(ROOT_PATH)
				.setValidator(this::assertInnerFields)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonElement data) {
		assertField(data,
			new NumberField
				.Builder("paidInstalments")
				.setMaxValue(2147483647)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("contractOutstandingBalance")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(4)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("releases")
				.setValidator(this::assertInnerReleases)
				.setMinItems(0)
				.build());
	}

	private void assertInnerReleases(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("paymentId")
				.setOptional()
				.setMaxLength(100)
				.setMinLength(1)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.build());

		assertField(body,
			new BooleanField
				.Builder("isOverParcelPayment")
				.build());

		assertField(body,
			new StringField
				.Builder("instalmentId")
				.setMaxLength(100)
				.setMinLength(1)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{0,99}$")
				.build());

		assertField(body,
			new DatetimeField
				.Builder("paidDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.build());

		assertField(body,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(body,
			new StringField
				.Builder("paidAmount")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(4)
				.build());

		assertField(body,
			new ObjectField
				.Builder("overParcel")
				.setValidator(this::assertInnerFieldOverParcel)
				.setOptional()
				.build());
	}

	private void assertInnerFieldOverParcel(JsonObject body) {
		assertField(body,
			new ObjectArrayField
				.Builder("fees")
				.setValidator(this::assertInnerFieldFees)
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("charges")
				.setValidator(this::assertInnerFieldsCharges)
				.setMinItems(0)
				.build());
	}

	private void assertInnerFieldFees(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("feeName")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("feeCode")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new StringField
				.Builder("feeAmount")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(4)
				.build());
	}

	private void assertInnerFieldsCharges(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("chargeType")
				.setEnums(ENUM_CHARGE_TYPE)
				.build());

		assertField(body,
			new StringField
				.Builder("chargeAmount")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(4)
				.build());

		assertField(body,
			new StringField
				.Builder("chargeAdditionalInfo")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());
	}
}
