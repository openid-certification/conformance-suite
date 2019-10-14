package net.openid.conformance.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import springfox.documentation.service.ApiListing;

import java.lang.reflect.Type;

public class SpringfoxApiListingJsonSerializer implements JsonSerializer<ApiListing> {

	@Override
	public JsonElement serialize(ApiListing apiListing, Type type, JsonSerializationContext context) {
		final JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty("apiVersion", apiListing.getApiVersion());
		jsonObject.addProperty("basePath", apiListing.getBasePath());
		jsonObject.addProperty("resourcePath", apiListing.getResourcePath());

		final JsonElement produces = context.serialize(apiListing.getProduces());
		jsonObject.add("produces", produces);

		final JsonElement consumes = context.serialize(apiListing.getConsumes());
		jsonObject.add("consumes", consumes);

		jsonObject.addProperty("host", apiListing.getHost());

		final JsonElement protocols = context.serialize(apiListing.getProtocols());
		jsonObject.add("protocols", protocols);

		final JsonElement securityReferences = context.serialize(apiListing.getSecurityReferences());
		jsonObject.add("securityReferences", securityReferences);

		final JsonElement apis = context.serialize(apiListing.getApis());
		jsonObject.add("apis", apis);

		final JsonElement models = context.serialize(apiListing.getModels());
		jsonObject.add("models", models);

		jsonObject.addProperty("description", apiListing.getDescription());
		jsonObject.addProperty("position", apiListing.getPosition());

		final JsonElement tags = context.serialize(apiListing.getTags());
		jsonObject.add("tags", tags);

		return jsonObject;
	}
}
