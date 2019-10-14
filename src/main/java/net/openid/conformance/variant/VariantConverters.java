package net.openid.conformance.variant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

public abstract class VariantConverters {

	private VariantConverters() {}

	public static Collection<Converter<?, ?>> getConverters() {
		return List.of(
				DocumentToVariantSelectionConverter.INSTANCE,
				StringToVariantSelectionConverter.INSTANCE,
				VariantSelectionToDocumentConverter.INSTANCE
		);
	}

	enum DocumentToVariantSelectionConverter implements Converter<Document, VariantSelection> {
		INSTANCE;

		public VariantSelection convert(Document source) {
			return new VariantSelection(
					source.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())));
		}
	}

	// Old tests in the DB may have plain-string variants
	enum StringToVariantSelectionConverter implements Converter<String, VariantSelection> {
		INSTANCE;

		public VariantSelection convert(String source) {
			return new VariantSelection(source);
		}
	}

	enum VariantSelectionToDocumentConverter implements Converter<VariantSelection, Document> {
		INSTANCE;

		@SuppressWarnings("unchecked")
		public Document convert(VariantSelection source) {
			return new Document((Map<String, Object>) (Map<String, ?>) source.getVariant());
		}
	}

}
