package net.openid.conformance.openbanking_brasil.creditOperations.advances.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openinsurance.validator.OpenBankingLinksAndMetaValidator;
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
 * Api: swagger/openinsurance/UnarrangedAccountsOverdraft/swagger-unarranged-accounts-overdraft.yaml
 * Api endpoint: /contracts/{contractId}/payments
 * Api version: 2.0.0-RC1.0
 * Git hash:
 */
@ApiName("Advances Payments V2")
public class AdvancesPaymentsResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final OpenBankingLinksAndMetaValidator linksAndMetaValidator = new OpenBankingLinksAndMetaValidator(this);

	final Set<String> CHARGE_TYPES = SetUtils.createSet("JUROS_REMUNERATORIOS_POR_ATRASO, MULTA_ATRASO_PAGAMENTO, JUROS_MORA_ATRASO, IOF_CONTRATACAO, IOF_POR_ATRASO, SEM_ENCARGO, OUTROS");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectField
				.Builder(ROOT_PATH)
				.setValidator(this::assertDataFields)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertDataFields(JsonObject data) {
		assertField(data,
			new NumberField
				.Builder("paidInstalments")
				.setMaxValue(999)
				.build());

		assertField(data,
			new StringField
				.Builder("contractOutstandingBalance")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(4)
				.setMaxLength(21)
				.build());

		assertField(data,
			new ObjectArrayField.Builder("releases")
				.setValidator(this::assertInnerFieldsForReleases)
				.setMinItems(0)
				.build());
	}

	private void assertInnerFieldsForReleases(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("paymentId")
				.setMaxLength(100)
				.setMinLength(1)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{1,100}$")
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
				.setMinLength(1)
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9-]{1,100}$")
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
			new StringField
				.Builder("paidAmount")
				.setMinLength(4)
				.setMaxLength(21)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.build());

		assertField(body,
			new ObjectField
				.Builder("overParcel")
				.setValidator(this::assertOverParcel)
				.setOptional()
				.build());
	}

	private void assertOverParcel(JsonObject body) {
		assertField(body,
			new ObjectArrayField
				.Builder("fees")
				.setValidator(this::assertOverParcelFees)
				.setMinItems(0)
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("charges")
				.setValidator(this::assertOverParcelCharges)
				.setMinItems(0)
				.build());
	}

	private void assertOverParcelCharges(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("chargeType")
				.setEnums(CHARGE_TYPES)
				.build());

		assertField(body,
			new StringField
				.Builder("chargeAdditionalInfo")
				.setMaxLength(140)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("chargeAmount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(4)
				.setMaxLength(21)
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
			new StringField
				.Builder("feeAmount")
				.setNullable()
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMinLength(4)
				.setMaxLength(21)
				.build());
	}
}
