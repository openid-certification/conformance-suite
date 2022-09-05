package net.openid.conformance.variant;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class VariantSelectionJsonSerializer implements JsonSerializer<VariantSelection> {

	@Override
	public JsonElement serialize(VariantSelection src, Type typeOfSrc, JsonSerializationContext context) {
		if (src.isLegacyVariant()) {
			return new JsonPrimitive(src.getLegacyVariant());
		} else {
			return context.serialize(src.getVariant());
		}
	}

}
