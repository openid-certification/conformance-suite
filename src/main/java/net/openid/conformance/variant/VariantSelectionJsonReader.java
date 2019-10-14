package net.openid.conformance.variant;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

@Component
public class VariantSelectionJsonReader implements Converter<String, VariantSelection> {

	private static final Gson gson = new Gson();

	@Override
	public VariantSelection convert(String source) {
		// Support test script (temporarily)
		if (!source.startsWith("{")) {
			return new VariantSelection(source);
		}
		return VariantSelection.fromJson(gson.fromJson(source, JsonElement.class));
	}

}
