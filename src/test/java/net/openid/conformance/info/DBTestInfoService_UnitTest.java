package net.openid.conformance.info;

import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DBTestInfoService_UnitTest {

	MongoTemplate mongoTemplate;
	TraceService traceService;
	DBTestInfoService service;

	@BeforeEach
	public void setUp() {
		mongoTemplate = Mockito.mock(MongoTemplate.class);
		traceService = Mockito.mock(TraceService.class);
		service = new DBTestInfoService();
		ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
		ReflectionTestUtils.setField(service, "traceService", traceService);
		Mockito.when(mongoTemplate.remove(any(Query.class), anyString())).thenReturn(DeleteResult.acknowledged(1));
	}

	@Test
	public void deletingTestsWithoutAnOwnerFilterDeletesTheTracesOfAllOfThem() {
		service.deleteTests(List.of("test1", "test2"), null);

		verify(traceService).deleteTracesForTestId("test1");
		verify(traceService).deleteTracesForTestId("test2");
		verifyNoMoreInteractions(traceService);
		// no need to ask which tests exist when all of them are deleted
		verify(mongoTemplate, never()).find(any(Query.class), eq(Document.class), anyString());
	}

	@Test
	public void deletingTestsOfAnOwnerOnlyDeletesTheTracesOfThatOwnersTests() {
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class), eq(DBTestInfoService.COLLECTION)))
			.thenReturn(List.of(new Document("_id", "mine")));

		service.deleteTests(List.of("mine", "somebodyElses"), Map.of("sub", "me", "iss", "https://idp.example.com"));

		verify(traceService).deleteTracesForTestId("mine");
		verifyNoMoreInteractions(traceService);
	}
}
