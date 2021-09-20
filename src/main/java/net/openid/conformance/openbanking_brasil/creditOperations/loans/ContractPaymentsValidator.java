package net.openid.conformance.openbanking_brasil.creditOperations.loans;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_loans_apis.yaml
 * Api endpoint: /contracts/{contractId}/payments
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Contract Payments")
public class ContractPaymentsValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
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
				.setMinLength(0)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
			 	.build());

		assertReleases(data);
	}

	private void assertReleases(JsonObject body) {
		assertHasField(body, "releases");
		assertJsonArrays(body, "releases", this::assertInnerFieldsForReleases);
	}

	private void assertInnerFieldsForReleases(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("paymentId")
				.setMaxLength(100)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
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
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
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
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertHasField(body, "overParcel");
		JsonObject overParcel = findByPath(body, "overParcel").getAsJsonObject();
		assertOverParcel(overParcel);
	}

	private void assertOverParcel(JsonObject body) {
		assertHasField(body,"fees");
		assertJsonArrays(body, "fees", this::assertOverParcelFees);

		assertHasField(body,"charges");
		assertJsonArrays(body, "charges", this::assertOverParcelCharges);
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
				.setMinLength(0)
				.setNullable()
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
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
				.setMinLength(0)
				.setNullable()
				.build());
	}
}
