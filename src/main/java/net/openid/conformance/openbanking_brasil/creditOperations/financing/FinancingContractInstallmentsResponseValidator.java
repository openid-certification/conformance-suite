package net.openid.conformance.openbanking_brasil.creditOperations.financing;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * API: swagger_financings_apis.yaml
 * URL: /contracts/{contractId}/scheduled-instalments
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Contract Installments")
public class FinancingContractInstallmentsResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> TYPE_NUMBER_OF_INSTALMENTS = Sets.newHashSet("DIA", "SEMANA",
		"MES", "ANO", "SEM_PRAZO_TOTAL");
	public static final Set<String> TYPE_CONTRACT_REMAINING = Sets.newHashSet("DIA", "SEMANA", "MES",
		"ANO", "SEM_PRAZO_REMANESCENTE");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);
		return environment;
	}

	private void assertInnerFields(JsonElement body) {
		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();

		assertField(data,
			new StringField
				.Builder("typeNumberOfInstalments")
				.setEnums(TYPE_NUMBER_OF_INSTALMENTS)
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
				.setEnums(TYPE_CONTRACT_REMAINING)
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
			new ObjectArrayField
				.Builder("balloonPayments")
				.setMinItems(0)
				.setNullable()
				.setValidator(this::assertInnerFieldsBalloonPayments)
				.build());
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
				.setMinLength(0)
				.setNullable()
				.build());
	}
}
