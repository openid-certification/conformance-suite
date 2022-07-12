package net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api: swagger_invoice_financings_apis.yaml
 * Api endpoint: /contracts/{contractId}/payments
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 *
 */
@ApiName("Invoice Financing Contract Payments")
public class InvoiceFinancingContractPaymentsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();
		assertDataFields(data);

		return environment;
	}

	private void assertDataFields(JsonObject data) {
		assertField(data,
			new IntField
				.Builder("paidInstalments")
				.setMaxLength(3)
				.setNullable()
				.build());

		assertField(data,
			new DoubleField
				.Builder("contractOutstandingBalance")
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.setMinLength(0)
				.build());

		assertReleases(data);
	}

	private void assertReleases(JsonObject body) {
		assertHasField(body, "releases");
		assertField(body, new ObjectArrayField.Builder("releases").setValidator(this::assertInnerFieldsForReleases).build());
	}

	private void assertInnerFieldsForReleases(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("paymentId")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("isOverParcelPayment")
				.build());

		assertField(body,
			new StringField
				.Builder("instalmentId")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new DatetimeField
				.Builder("paidDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(body,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.build());

		assertField(body,
			new DoubleField
				.Builder("paidAmount")
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.build());

		assertHasField(body, "overParcel");
		JsonObject overParcel = findByPath(body, "overParcel").getAsJsonObject();
		assertOverParcel(overParcel);
	}

	private void assertOverParcel(JsonObject body) {
		assertHasField(body,"fees");
		assertField(body, new ObjectArrayField.Builder("fees").setValidator(this::assertOverParcelFees).build());

		assertHasField(body,"charges");
		assertField(body, new ObjectArrayField.Builder("charges").setValidator(this::assertOverParcelCharges).build());
	}

	private void assertOverParcelCharges(JsonObject body) {
		final Set<String> chargeTypes = Set.of("JUROS_REMUNERATORIOS_POR_ATRASO",
			"MULTA_ATRASO_PAGAMENTO", "JUROS_MORA_ATRASO", "IOF_CONTRATACAO",
			"IOF_POR_ATRASO", "SEM_ENCARGO", "OUTROS");

		assertField(body,
			new StringField
				.Builder("chargeType")
				.setMaxLength(31)
				.setEnums(chargeTypes)
				.build());

		assertField(body,
			new StringField
				.Builder("chargeAdditionalInfo")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(body,
			new DoubleField
				.Builder("chargeAmount")
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.setMinLength(0)
				.setNullable()
				.build());
	}

	private void assertOverParcelFees(JsonObject body) {
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
			new DoubleField
				.Builder("feeAmount")
				.setNullable()
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.setMinLength(0)
				.setNullable()
				.build());
	}
}