package net.openid.conformance.openbanking_brasil.opendata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

public class CommonOpendataParts {

	private final AbstractJsonAssertingCondition validator;

	public CommonOpendataParts(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
	}

	private static class Fields extends ProductNServicesCommonFields {
	}

	public void assertSocietyIdentification(JsonObject societyIdentification) {
		validator.assertField(societyIdentification, Fields.name().setMaxLength(80).build());
		validator.assertField(societyIdentification, Fields.cnpjNumber().setPattern("^\\d{14}$").build());
	}

	public void assertParticipantIdentification(JsonObject participantIdentification) {
		validator.assertField(participantIdentification,
			new StringField
				.Builder("brand")
				.setMaxLength(80)
				.build());

		validator.assertField(participantIdentification, Fields.name().setMaxLength(80).build());
		validator.assertField(participantIdentification, Fields.cnpjNumber().setPattern("^\\d{14}$").build());

		validator.assertField(participantIdentification,
			new StringField
				.Builder("urlComplementaryList")
				.setMaxLength(1024)
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setOptional()
				.build());

	}

	public void assertValue(JsonObject minValue) {
		validator.assertField(minValue,
			new StringField
				.Builder("amount")
				.setMaxLength(9)
				.build());

		validator.assertField(minValue,
			new ObjectField
				.Builder("unit")
				.setValidator(this::assertUnit)
				.build());
	}

	public void assertUnit(JsonObject unit) {
		validator.assertField(unit,
			new StringField
				.Builder("code")
				.setMaxLength(4)
				.build());

		validator.assertField(unit,
			new StringField
				.Builder("description")
				.setMaxLength(80)
				.build());
	}

}
