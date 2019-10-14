package net.openid.conformance;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import net.openid.conformance.serializers.SpringfoxApiListingJsonSerializer;
import net.openid.conformance.serializers.SpringfoxJsonSerializer;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantSelectionJsonSerializer;

import org.bson.Document;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import springfox.documentation.service.ApiListing;
import springfox.documentation.spring.web.json.Json;

public class CollapsingGsonHttpMessageConverter extends GsonHttpMessageConverter {

	/**
	 *
	 */
	public CollapsingGsonHttpMessageConverter() {
		super();
		setGson(getDbObjectCollapsingGson());
	}

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractGenericHttpMessageConverter#supports(java.lang.Class)
	 */
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		if (JsonElement.class.isAssignableFrom(clazz)) {
			// if we are converting to a JsonElement, of some type, then go ahead
			return canRead(mediaType);
		} else {
			// otherwise, don't do it
			return false;
		}
	}

	/**
	 * Special GSON converter that looks for and collapses __wrapped_key_element fields
	 *
	 * @return
	 */
	public static Gson getDbObjectCollapsingGson() {
		return new GsonBuilder()
			.registerTypeHierarchyAdapter(Document.class, new JsonSerializer<Document>() {

				private Gson internalGson = new Gson();

				@Override
				public JsonElement serialize(Document src, Type typeOfSrc, JsonSerializationContext context) {
					// run the field conversion
					Object converted = convertStructureToField(src);
					// delegate to regular GSON for the real work
					return internalGson.toJsonTree(converted);
				}

				private Object convertStructureToField(Object source) {
					if (source instanceof List) {
						// if it's a list of some type, loop through it
						@SuppressWarnings("unchecked")
						List<Object> list = (List<Object>) source;
						List<Object> converted = list.stream()
							.map(this::convertStructureToField)
							.collect(Collectors.toList());
						return converted;
					} else if (source instanceof Document) {
						// if it's an object, need to look through all the fields and convert any weird ones
						Document dbo = (Document) source;
						Document converted = new Document();
						for (String key : dbo.keySet()) {
							if (key.startsWith("__wrapped_key_element_")) {
								Document wrapped = dbo.get(key, Document.class);
								converted.put((String) wrapped.get("key"), convertStructureToField(wrapped.get("value")));
							} else if (key.equals("_class")) {
								// skip all class elements

							} else {
								converted.put(key, convertStructureToField(dbo.get(key)));
							}
						}
						return converted;
					} else {
						return source;
					}
				}
			})
			// needed for making calls to /v2/api-docs
			.registerTypeAdapter(Json.class, new SpringfoxJsonSerializer())
			// needed for making calls to /swagger-ui.html
			.registerTypeAdapter(ApiListing.class, new SpringfoxApiListingJsonSerializer())
			// needed for variants
			.registerTypeAdapter(VariantSelection.class, new VariantSelectionJsonSerializer())
			.create();
	}

}
