package net.openid.conformance;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.logging.DBEventLog;
import net.openid.conformance.logging.TestInstanceEventLog;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

public class BaseConditionMocked_UnitTest {
	@Mock
	private MongoTemplate mongoTemplate;
	private ImmutableMap<String, String> owner = ImmutableMap.<String, String>builder()
		.put("user", "something").build();
//	public @Rule
//	ExpectedException exception = ExpectedException.none();
	protected TestInstanceEventLog eventLog = new TestInstanceEventLog("test-id,", owner, new MockedDBEventLog(mongoTemplate));
	MappingMongoConverter converter;
	MongoMappingContext mappingContext;
	@Mock
	ApplicationContext context;
	@Mock
	DbRefResolver resolver;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUpDbEventLogMock() throws Exception {

		mappingContext = new MongoMappingContext();
		mappingContext.setApplicationContext(context);
		mappingContext.afterPropertiesSet();

		MongoCustomConversions mongoCustomConversions = new ApplicationConfig().mongoCustomConversions();

		converter = new MappingMongoConverter(resolver, mappingContext);
		converter.setCustomConversions(mongoCustomConversions);
		converter.afterPropertiesSet();

	}

	public class MockedDBEventLog extends DBEventLog {
		public MockedDBEventLog(MongoTemplate mongoTemplate) {
			super(mongoTemplate);
		}

		@Override
		protected <T> void insert(T objectToSave) {
			org.bson.Document document = new org.bson.Document();
			converter.write(objectToSave, document);
		}
	}
}
