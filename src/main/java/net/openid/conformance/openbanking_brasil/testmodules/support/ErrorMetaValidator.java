package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.*;

public abstract class ErrorMetaValidator extends AbstractJsonAssertingCondition {
	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectField
				.Builder("meta")
				.setValidator(this::assertMeta)
				.setOptional()
				.build());

		assertField(body,
			new ObjectArrayField
				.Builder("errors")
				.setValidator(this::assertError)
				.setMinItems(1)
				.setMaxItems(13)
				.build());

		return environment;
	}

	private void assertMeta(JsonObject meta) {
		assertField(meta,
			new IntField
				.Builder("totalRecords")
				.build());

		assertField(meta,
			new IntField
				.Builder("totalPages")
				.build());

		assertField(meta,
			new DatetimeField
				.Builder("requestDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());
	}

	private void assertError(JsonObject error) {
		String pattern = "[\\w\\W\\s]*";

		assertField(error,
			new StringField
				.Builder("code")
				.setPattern(pattern)
				.setMaxLength(255)
				.build());

		assertField(error,
			new StringField
				.Builder("title")
				.setPattern(pattern)
				.setMaxLength(255)
				.build());

		assertField(error,
			new StringField
				.Builder("detail")
				.setPattern(pattern)
				.setMaxLength(2048)
				.build());

	}

	@Override
	protected JsonElement bodyFrom(Environment environment) {
		if(isResource()){
			return super.bodyFrom(environment);
		}
		String resource = environment.getString("consent_endpoint_response_full", "body");
		return GSON.fromJson(resource, JsonElement.class);
	}

	abstract protected boolean isResource();
}
