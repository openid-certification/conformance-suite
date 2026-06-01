package net.openid.conformance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.openid.conformance.info.ConfigMigratingResponse;
import net.openid.conformance.info.ConfigMigration;
import net.openid.conformance.logging.MongoKeyWrapper;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantSelectionJsonSerializer;
import org.bson.Document;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import java.lang.reflect.Type;

public final class CollapsingGsonHttpMessageConverter extends GsonHttpMessageConverter {

	/**
	 * Reused across every {@link Document} serialization. Held as a static so a full-log fetch
	 * (which serializes one entry per event) doesn't allocate a fresh Gson per entry.
	 */
	private static final Gson INTERNAL_GSON = new GsonBuilder().serializeNulls().create();

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
	 * Special GSON converter that looks for and collapses {@code __wrapped_key_element_*}
	 * envelopes via {@link MongoKeyWrapper#unwrap}.
	 */
	public static Gson getDbObjectCollapsingGson() {
		return getDbObjectCollapsingGson(false);
	}

	public static Gson getDbObjectCollapsingGson(boolean prettyPrint) {
		GsonBuilder gsonBuilder = new GsonBuilder()
			.registerTypeHierarchyAdapter(Document.class,
				(JsonSerializer<Document>) (src, typeOfSrc, context) ->
					INTERNAL_GSON.toJsonTree(MongoKeyWrapper.unwrap(src)))

			// needed for variants
			.registerTypeAdapter(VariantSelection.class, new VariantSelectionJsonSerializer())

			// Single-pass legacy-key migration on responses whose top-level JSON has a
			// "config" object that should receive ConfigMigration. The serializer delegates
			// inner-object serialization to the normal Gson pipeline (Document collapsing
			// included) and then mutates the resulting tree in place. Callers wrap the body
			// via ConfigMigratingResponse so Spring picks up the wrapped type.
			.registerTypeAdapter(ConfigMigratingResponse.class, new JsonSerializer<ConfigMigratingResponse>() {
				@Override
				public JsonElement serialize(ConfigMigratingResponse src, Type typeOfSrc, JsonSerializationContext context) {
					JsonElement el = context.serialize(src.inner());
					if (el != null && el.isJsonObject()) {
						JsonObject obj = el.getAsJsonObject();
						JsonElement configEl = obj.get("config");
						if (configEl != null && configEl.isJsonObject()) {
							ConfigMigration.migrateLegacyClientAttestationKeys(configEl.getAsJsonObject());
						}
					}
					return el;
				}
			});

		if (prettyPrint) {
			gsonBuilder.setPrettyPrinting();
		}
		gsonBuilder.serializeNulls();
		return gsonBuilder.create();
	}

}
