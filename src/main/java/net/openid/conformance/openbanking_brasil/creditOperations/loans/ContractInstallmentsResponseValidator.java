package net.openid.conformance.openbanking_brasil.creditOperations.loans;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger_loans_apis.yaml
 * Api endpoint: /contracts/{contractId}/scheduled-instalments
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Contract Installments")
public class ContractInstallmentsResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_TYPE_NUMBER_OF_INSTALMENTS = Sets.newHashSet("DIA", "SEMANA", "MES", "ANO", "SEM_PRAZO_TOTAL");
	public static final Set<String> ENUM_TYPE_CONTRACT_REMAINING = Sets.newHashSet("DIA", "SEMANA", "MES", "ANO", "SEM_PRAZO_REMANESCENTE");

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

		assertField(data,
			new StringField
				.Builder("typeNumberOfInstalments")
				.setEnums(ENUM_TYPE_NUMBER_OF_INSTALMENTS)
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
				.setEnums(ENUM_TYPE_CONTRACT_REMAINING)
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

	private void assertBalloonPayments(JsonObject data) {
		assertField(data,
			new ObjectArrayField
				.Builder("balloonPayments")
				.setValidator(this::assertInnerFieldsBalloonPayments)
				.setMinItems(0)
				.setNullable()
				.build());
	}

	private void assertInnerFieldsBalloonPayments(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("dueDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
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
				.setPattern("^-?\\d{1,15}(\\.\\d{1,4})?$")
				.setMinLength(0)
				.setNullable()
				.build());
	}
}
