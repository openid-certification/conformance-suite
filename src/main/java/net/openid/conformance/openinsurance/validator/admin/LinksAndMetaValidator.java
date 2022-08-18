package net.openid.conformance.openinsurance.validator.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

public class LinksAndMetaValidator {

	private final AbstractJsonAssertingCondition validator;

	public LinksAndMetaValidator(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
	}

	public void assertMetaAndLinks(JsonElement body) {
		validator.assertField(body,
			new ObjectField
				.Builder("links")
				.setValidator(this::assertLinks)
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
				.setOptional()
				.build());

		validator.assertField(links,
			new StringField
				.Builder("first")
				.setOptional()
				.build());

		validator.assertField(links,
			new StringField
				.Builder("prev")
				.setOptional()
				.build());

		validator.assertField(links,
			new StringField
				.Builder("next")
				.setOptional()
				.build());

		validator.assertField(links,
			new StringField
				.Builder("last")
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
	}
}
