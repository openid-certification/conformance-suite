package net.openid.conformance.openbanking_brasil.creditOperations.financing.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.creditOperations.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.NumberField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openinsurance/financings/v2/swagger_financings_apis-v2.yaml
 * Api endpoint: /contracts/{contractId}/scheduled-instalments
 * Api version: 2.0.0.final
 */

@ApiName("Financings Scheduled Instalments V2")
public class FinancingContractInstallmentsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> TYPE_NUMBER_OF_INSTALMENTS = SetUtils.createSet("DIA, SEMANA, MES, ANO, SEM_PRAZO_TOTAL");
	public static final Set<String> TYPE_CONTRACT_REMAINING = SetUtils.createSet("DIA, SEMANA, MES, ANO, SEM_PRAZO_REMANESCENTE");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
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
			new StringField
				.Builder("typeNumberOfInstalments")
				.setEnums(TYPE_NUMBER_OF_INSTALMENTS)
				.build());

		assertField(data,
			new NumberField
				.Builder("totalNumberOfInstalments")
				.setMaxValue(999999)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("typeContractRemaining")
				.setEnums(TYPE_CONTRACT_REMAINING)
				.build());

		assertField(data,
			new NumberField
				.Builder("contractRemainingNumber")
				.setMaxLength(999999)
				.setOptional()
				.build());

		assertField(data,
			new NumberField
				.Builder("paidInstalments")
				.setMaxLength(999)
				.build());

		assertField(data,
			new NumberField
				.Builder("dueInstalments")
				.setMaxLength(999)
				.build());

		assertField(data,
			new NumberField
				.Builder("pastDueInstalments")
				.setMaxLength(999)
				.build());

		assertField(data,
			new ObjectArrayField
				.Builder("balloonPayments")
				.setMinItems(1)
				.setValidator(this::assertInnerFieldsBalloonPayments)
				.setOptional()
				.build());
	}

	private void assertInnerFieldsBalloonPayments(JsonObject body) {
		assertField(body,
			new DatetimeField
				.Builder("dueDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
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
			new StringField
				.Builder("amount")
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
