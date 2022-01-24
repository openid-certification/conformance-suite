package net.openid.conformance.openbanking_brasil.opendata.pension;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.util.field.StringField;

public class CommonOpenDataParts {

	private final AbstractJsonAssertingCondition validator;

	private static class Fields extends ProductNServicesCommonFields {
	}

	public CommonOpenDataParts(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
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

}
