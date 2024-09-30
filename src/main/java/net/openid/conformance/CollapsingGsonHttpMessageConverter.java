package net.openid.conformance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.openid.conformance.logging.GsonObjectToBsonDocumentConverter;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantSelectionJsonSerializer;
import org.bson.Document;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public final class CollapsingGsonHttpMessageConverter extends GsonHttpMessageConverter {

	public CollapsingGsonHttpMessageConverter() {
		super();
		this.setGson(getDbObjectCollapsingGson());
	}

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
		return getDbObjectCollapsingGson(false);
	}
	/**
	 * Special GSON converter that looks for and collapses __wrapped_key_element fields
	 *
	 * @return
	 */
	public static Gson getDbObjectCollapsingGson(boolean prettyPrint) {
		GsonBuilder gsonBuilder = new GsonBuilder()
			.registerTypeHierarchyAdapter(Document.class, new JsonSerializer<Document>() {

				private Gson internalGson = new GsonBuilder().serializeNulls().create();

				@Override
				public JsonElement serialize(Document src, Type typeOfSrc, JsonSerializationContext context) {
					// run the field conversion
					Object converted = convertStructureToField(src);
					// delegate to regular GSON for the real work
					return internalGson.toJsonTree(converted);
				}

				private Object convertStructureToField(Object source) {
					if (source instanceof List<?> list) {
						List<Object> converted = list.stream()
							.map(this::convertStructureToField)
							.collect(Collectors.toList());
						return converted;
					} else if (source instanceof Document dbo) {
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
						if(GsonObjectToBsonDocumentConverter.CONFORMANCE_SUITE_JSON_NULL_CONSTANT.equals(source)){
							return JsonNull.INSTANCE;
						} else {
							return source;
						}
					}
				}
			})

			// needed for variants
			.registerTypeAdapter(VariantSelection.class, new VariantSelectionJsonSerializer());
		if(prettyPrint) {
			gsonBuilder.setPrettyPrinting();
		}
		gsonBuilder.serializeNulls();
		return gsonBuilder.create();
	}

}
