package net.openid.conformance.variant;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class VariantSelectionJsonReader implements Converter<String, VariantSelection> {

	private static final Gson gson = new Gson();

	@Override
	public VariantSelection convert(String source) {
		return VariantSelection.fromJson(gson.fromJson(source, JsonElement.class));
	}

}
