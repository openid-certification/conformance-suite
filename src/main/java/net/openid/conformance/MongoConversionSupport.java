package net.openid.conformance;

import net.openid.conformance.logging.GsonArrayToBsonArrayConverter;
import net.openid.conformance.logging.GsonObjectToBsonDocumentConverter;
import net.openid.conformance.logging.GsonPrimitiveToBsonValueConverter;
import net.openid.conformance.variant.VariantConverters;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.List;

public final class MongoConversionSupport {

	private MongoConversionSupport() {}

	public static MongoCustomConversions createMongoCustomConversions() {
		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(new GsonPrimitiveToBsonValueConverter());
		converters.add(new GsonObjectToBsonDocumentConverter());
		converters.add(new GsonArrayToBsonArrayConverter());
		converters.addAll(VariantConverters.getConverters());
		return new MongoCustomConversions(converters);
	}
}
