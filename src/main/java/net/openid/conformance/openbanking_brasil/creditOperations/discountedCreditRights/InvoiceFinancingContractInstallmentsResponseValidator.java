package net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_invoice_financings_apis.yaml
 * Api endpoint: /contracts/{contractId}/scheduled-instalments
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 *
 */

@ApiName("Invoice Financing Contract Installments")
public class InvoiceFinancingContractInstallmentsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();
		final Set<String> typeNumberOfInstalments = Sets.newHashSet("DIA", "SEMANA",
			"MES", "ANO", "SEM_PRAZO_TOTAL");
		final Set<String> typeContractRemaining = Sets.newHashSet("DIA", "SEMANA", "MES",
			"ANO", "SEM_PRAZO_REMANESCENTE");

		assertField(data,
			new StringField
				.Builder("typeNumberOfInstalments")
				.setEnums(typeNumberOfInstalments)
				.setMaxLength(15)
				.build());

		assertField(data,
			new IntField
				.Builder("totalNumberOfInstalments")
				.setMaxLength(6)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("typeContractRemaining")
				.setEnums(typeContractRemaining)
				.setMaxLength(22)
				.build());

		assertField(data,
			new IntField
				.Builder("contractRemainingNumber")
				.setMaxLength(6)
				.setNullable()
				.build());

		assertField(data,
			new IntField
				.Builder("paidInstalments")
				.setMaxLength(3)
				.setNullable()
				.build());

		assertField(data,
			new IntField
				.Builder("dueInstalments")
				.setMaxLength(3)
				.setNullable()
				.build());

		assertField(data,
			new IntField
				.Builder("pastDueInstalments")
				.setMaxLength(3)
				.setNullable()
				.build());

		assertBalloonPayments(data);
	}

	private void assertBalloonPayments(JsonObject body) {
		assertField(body,
			new ArrayField
				.Builder("balloonPayments")
				.setMinItems(0)
				.setNullable()
				.build());

		assertJsonArrays(body, "balloonPayments", this::assertInnerFieldsBalloonPayments);
	}

	private void assertInnerFieldsBalloonPayments(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("dueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(body,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$|^NA$")
				.setMaxLength(3)
				.build());

		assertField(body,
			new DoubleField.
				Builder("amount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(0)
				.setNullable()
				.build());
	}
}
