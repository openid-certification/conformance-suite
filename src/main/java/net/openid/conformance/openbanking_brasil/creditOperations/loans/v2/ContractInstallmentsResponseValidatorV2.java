package net.openid.conformance.openbanking_brasil.creditOperations.loans.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenBankingLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api: swagger/openinsurance/loansV2/swagger_loans_apis.yaml
 * Api endpoint: /contracts/{contractId}/scheduled-instalments
 * Api version: 2.0.0-RC1.0
 * Git hash:
 */

@ApiName("Contract Installments V2")
public class ContractInstallmentsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final OpenBankingLinksAndMetaValidator linksAndMetaValidator = new OpenBankingLinksAndMetaValidator(this);
	public static final Set<String> ENUM_TYPE_NUMBER_OF_INSTALMENTS = SetUtils.createSet("DIA, SEMANA, MES, ANO, SEM_PRAZO_TOTAL");
	public static final Set<String> ENUM_TYPE_CONTRACT_REMAINING = SetUtils.createSet("DIA, SEMANA, MES, ANO, SEM_PRAZO_REMANESCENTE");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertInnerFields(body);
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertInnerFields(JsonElement body) {
		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();

		assertField(data,
			new StringField
				.Builder("typeNumberOfInstalments")
				.setEnums(ENUM_TYPE_NUMBER_OF_INSTALMENTS)
				.build());

		assertField(data,
			new IntField
				.Builder("totalNumberOfInstalments")
				.setMaxLength(999999999)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("typeContractRemaining")
				.setEnums(ENUM_TYPE_CONTRACT_REMAINING)
				.build());

		assertField(data,
			new IntField
				.Builder("contractRemainingNumber")
				.setMaxLength(999999999)
				.setOptional()
				.build());

		assertField(data,
			new IntField
				.Builder("paidInstalments")
				.setMaxLength(999)
				.build());

		assertField(data,
			new IntField
				.Builder("dueInstalments")
				.setMaxLength(999)
				.build());

		assertField(data,
			new IntField
				.Builder("pastDueInstalments")
				.setMaxLength(999)
				.build());

		assertBalloonPayments(data);
	}

	private void assertBalloonPayments(JsonObject data) {
		assertField(data,
			new ObjectArrayField
				.Builder("balloonPayments")
				.setValidator(this::assertInnerFieldsBalloonPayments)
				.setMinItems(1)
				.setOptional()
				.build());
	}

	private void assertInnerFieldsBalloonPayments(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("dueDate")
				.setMaxLength(10)
				.setMinLength(2)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("amount")
				.setValidator(this::assertAmount)
				.build());
	}

	private void assertAmount(JsonObject amount) {
		assertField(amount,
			new StringField.
				Builder("amount")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(4)
				.setMaxLength(20)
				.build());

		assertField(amount,
			new StringField
				.Builder("currency")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.build());
	}
}
