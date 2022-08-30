package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.LinksAndMetaOnlyRequestDateTimeValidator;

import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonUtils;
import net.openid.conformance.util.field.*;

public abstract class AbstractErrorMetaValidator extends AbstractJsonAssertingCondition {
	private static final Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		String isMetaOnlyRequestDateTime = environment.getString("metaOnlyRequestDateTime");

		if(!Strings.isNullOrEmpty(isMetaOnlyRequestDateTime) && isMetaOnlyRequestDateTime.equals("true")){
			LinksAndMetaOnlyRequestDateTimeValidator linksAndMetaOnlyRequestDateTimeValidator = new LinksAndMetaOnlyRequestDateTimeValidator(this);
			linksAndMetaOnlyRequestDateTimeValidator.assertMeta(body.getAsJsonObject());
		}else {
			LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
			linksAndMetaValidator.assertMeta(body.getAsJsonObject());
		}

		assertField(body,
			new ObjectArrayField
				.Builder("errors")
				.setValidator(this::assertError)
				.setMinItems(1)
				.setMaxItems(13)
				.build());

		return environment;
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

	protected abstract boolean isResource();
}
