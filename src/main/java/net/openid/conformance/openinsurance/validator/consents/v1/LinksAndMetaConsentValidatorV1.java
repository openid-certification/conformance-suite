package net.openid.conformance.openinsurance.validator.consents.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

public class LinksAndMetaConsentValidatorV1 {

	private final AbstractJsonAssertingCondition validator;

	public LinksAndMetaConsentValidatorV1(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
	}

	public void assertMetaAndLinks(JsonElement body) {
		validator.assertField(body,
			new ObjectField
				.Builder("links")
				.setValidator(this::assertLinks)
				.setOptional()
				.build());

		validator.assertField(body,
			new ObjectField
				.Builder("meta")
				.setValidator(this::assertMeta)
				.setOptional()
				.build());
	}

	public void assertLinks(JsonObject links) {
		validator.assertField(links,
			new StringField
				.Builder("self")
				.setMaxLength(2000)
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.build());

		validator.assertField(links,
			new StringField
				.Builder("first")
				.setOptional()
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setMaxLength(2000)
				.build());

		validator.assertField(links,
			new StringField
				.Builder("prev")
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setMaxLength(2000)
				.setOptional()
				.build());

		validator.assertField(links,
			new StringField
				.Builder("next")
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setMaxLength(2000)
				.setOptional()
				.build());

		validator.assertField(links,
			new StringField
				.Builder("last")
				.setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
				.setMaxLength(2000)
				.setOptional()
				.build());
	}

	public void assertMeta(JsonObject meta) {
		validator.assertField(meta,
			new IntField
				.Builder("totalRecords")
				.build());

		validator.assertField(meta,
			new IntField
				.Builder("totalPages")
				.build());

		validator.assertField(meta,
			new DatetimeField
				.Builder("requestDateTime")
				.setMaxLength(20)
				.build());
	}
}
