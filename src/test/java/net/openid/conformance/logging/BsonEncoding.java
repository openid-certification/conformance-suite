package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import com.mongodb.MongoClientSettings;
import net.openid.conformance.MongoConversionSupport;
import org.bson.BsonBinaryWriter;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test helper that drives the same encode path {@code DBEventLog} uses, without a live MongoDB.
 *
 * <p>Catches the class of regression where a value placed into a log payload (via
 * {@code args(...)} / {@code log(String, Map)} / {@code log(String, JsonObject)}) has no BSON
 * codec. In production that blows up inside {@code mongoTemplate.insert(...)}; here we reproduce
 * the same encode step so it fails at unit-test time instead.
 *
 * <p>Production's {@code mongoTemplate.insert(dbObject, collection)} calls
 * {@code MappingMongoConverter.write(dbObject, document)} to apply the
 * {@link MongoCustomConversions} registered in
 * {@link MongoConversionSupport#createMongoCustomConversions()} before the MongoDB driver encodes
 * the resulting {@link Document} via {@link DocumentCodec}. This helper uses the real
 * {@link MappingMongoConverter} with that same shared conversion setup, so it stays aligned with
 * production over time.
 */
public final class BsonEncoding {

	private static final MappingMongoConverter MAPPING_MONGO_CONVERTER = buildMappingMongoConverter();

	private BsonEncoding() {}

	/**
	 * Build a {@link TestInstanceEventLog} for use in a condition's {@code _UnitTest} instead of
	 * a Mockito mock. Every {@code log(...)} call made by the condition under test is routed
	 * through {@link #assertEncodable(Map)} / {@link #assertEncodable(JsonObject)}, so any
	 * un-encodable value in a log payload fails the test immediately with a clear message.
	 *
	 * <p>Adoption in an existing {@code _UnitTest}: remove the {@code @Mock TestInstanceEventLog}
	 * field and replace it with
	 * <pre>
	 *   private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	 * </pre>
	 * The rest of the test class (including the {@code cond.setProperties(..., eventLog, ...)}
	 * call) stays identical.
	 */
	public static TestInstanceEventLog testInstanceEventLog() {
		TestInstanceEventLog real = new TestInstanceEventLog("UNIT-TEST", Map.of(), new EventLog() {
			@Override
			public void log(String testId, String source, Map<String, String> owner, String msg) {
				// Plain-string log entries don't exercise BSON codec lookup beyond strings.
			}

			@Override
			public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {
				assertEncodable(obj);
			}

			@Override
			public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {
				assertEncodable(map);
			}

			@Override
			public void createIndexes() {
			}
		});
		return real;
	}

	/**
	 * Mirror of {@code DBEventLog.log(String, String, Map, Map)} minus the
	 * {@code mongoTemplate.insert(...)}. Runs the supplied map through
	 * {@link GsonArrayToBsonArrayConverter#convertUnloggableValuesInMap(Map)} (the pre-pass
	 * {@code DBEventLog} applies before insertion), then through a real
	 * {@link MappingMongoConverter} configured with the same {@link MongoCustomConversions} as
	 * production, then encodes the resulting {@link Document} via {@link DocumentCodec}.
	 *
	 * <p>Fails the test with a message naming the offending Java type if any value in the map
	 * has no registered BSON codec.
	 */
	public static void assertEncodable(Map<String, Object> input) {
		Map<String, Object> post = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(input);
		Document doc = new Document();
		if (post != null) {
			MAPPING_MONGO_CONVERTER.write(post, doc);
		}
		encodeAsBson(doc, input);
	}

	/**
	 * Mirror of {@code DBEventLog.log(String, String, Map, JsonObject)} minus the
	 * {@code mongoTemplate.insert(...)}. {@code DBEventLog} converts the JsonObject to a
	 * pure-BSON Document via {@link GsonObjectToBsonDocumentConverter#convertFieldsToStructure}
	 * + {@link Document#parse(String)} before insertion, so the resulting Document is
	 * already free of Gson types; we encode it directly.
	 */
	public static void assertEncodable(JsonObject input) {
		Document doc = input == null
			? new Document()
			: Document.parse(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(input).toString());
		encodeAsBson(doc, input);
	}

	private static void encodeAsBson(Document doc, Object originalInput) {
		// Use the same default codec registry MongoClient/MongoTemplate use in production —
		// this includes providers like EnumCodecProvider that a bare `new DocumentCodec()`
		// would miss, keeping the helper faithful to the production encode path.
		CodecRegistry registry = MongoClientSettings.getDefaultCodecRegistry();
		BasicOutputBuffer buffer = new BasicOutputBuffer();
		try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
			new DocumentCodec(registry).encode(writer, doc, EncoderContext.builder().build());
		} catch (CodecConfigurationException ex) {
			fail("BSON encoding failed for log payload " + describe(originalInput)
				+ " (same DocumentCodec path as DBEventLog.log): " + ex.getMessage(), ex);
		}
	}

	private static MappingMongoConverter buildMappingMongoConverter() {
		MongoCustomConversions conversions = MongoConversionSupport.createMongoCustomConversions();
		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		mappingContext.afterPropertiesSet();
		MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
		converter.setCustomConversions(conversions);
		converter.afterPropertiesSet();
		return converter;
	}

	private static String describe(Object input) {
		if (input == null) {
			return "<null>";
		}
		String rendered = input.toString();
		if (rendered.length() > 500) {
			rendered = rendered.substring(0, 500) + "...";
		}
		return rendered;
	}
}
