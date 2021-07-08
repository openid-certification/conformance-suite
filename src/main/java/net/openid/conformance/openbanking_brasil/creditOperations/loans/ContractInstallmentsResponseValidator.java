package net.openid.conformance.openbanking_brasil.creditOperations.loans;

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
 * This is validator for API - Empréstimos - Parcelas do Contrato
 * https://openbanking-brasil.github.io/areadesenvolvedor/#emprestimos-parcelas-do-contrato
 */

@ApiName("Contract Installments")
public class ContractInstallmentsResponseValidator extends AbstractJsonAssertingCondition {

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
		Set<String> enumTypeNumberOfInstalments = Sets.newHashSet("DIA", "SEMANA", "MES", "ANO", "SEM_PRAZO_TOTAL");
		Set<String> enumTypeContractRemaining = Sets.newHashSet("DIA", "SEMANA", "MES", "ANO", "SEM_PRAZO_REMANESCENTE");

		assertField(data,
			new StringField
				.Builder("typeNumberOfInstalments")
				.setEnums(enumTypeNumberOfInstalments)
				.setMaxLength(6)
				.build());

		assertField(data,
			new IntField
				.Builder("totalNumberOfInstalments")
				.setMaxLength(6)
				.build());

		assertField(data,
			new StringField
				.Builder("typeContractRemaining")
				.setEnums(enumTypeContractRemaining)
				.setMaxLength(6)
				.build());

		assertField(data,
			new IntField
				.Builder("contractRemainingNumber")
				.setMaxLength(6)
				.build());

		assertField(data,
			new IntField
				.Builder("paidInstalments")
				.setMaxLength(3)
				.build());

		assertField(data,
			new IntField
				.Builder("dueInstalments")
				.setMaxLength(3)
				.build());

		assertField(data,
			new IntField
				.Builder("pastDueInstalments")
				.setMaxLength(3)
				.build());

		assertBalloonPayments(data);
	}

	private void assertBalloonPayments(JsonObject data) {
		assertField(data,
			new ArrayField
				.Builder("balloonPayments")
				.setMinItems(0).build());

		assertJsonArrays(data, "balloonPayments", this::assertInnerFieldsBalloonPayments);
	}

	private void assertInnerFieldsBalloonPayments(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("dueDate")
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
				.setMinLength(0)
				.build());
	}
}
