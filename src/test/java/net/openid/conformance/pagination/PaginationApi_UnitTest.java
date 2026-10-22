package net.openid.conformance.pagination;

import net.openid.conformance.info.DBTestPlanService;
import net.openid.conformance.info.PlanRepository;
import net.openid.conformance.info.TestInfoRepository;
import net.openid.conformance.info.TestPlanApi;
import net.openid.conformance.logging.LogApi;
import net.openid.conformance.security.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaginationApi_UnitTest {

	private MockMvc mvc;
	private TestInfoRepository testInfos;
	private PlanRepository plans;

	@BeforeEach
	void setUp() {
		testInfos = mock(TestInfoRepository.class);
		plans = mock(PlanRepository.class);
		AuthenticationFacade authentication = mock(AuthenticationFacade.class);
		DBTestPlanService planService = new DBTestPlanService();
		ReflectionTestUtils.setField(planService, "plans", plans);
		ReflectionTestUtils.setField(planService, "authenticationFacade", authentication);
		TestPlanApi planApi = new TestPlanApi();
		ReflectionTestUtils.setField(planApi, "planService", planService);
		LogApi logApi = new LogApi();
		ReflectionTestUtils.setField(logApi, "testInfos", testInfos);
		ReflectionTestUtils.setField(logApi, "authenticationFacade", authentication);
		mvc = MockMvcBuilders.standaloneSetup(planApi, logApi).build();
	}

	@ParameterizedTest
	@CsvSource({
		"/api/log, true, 15, 10", "/api/plan, true, 15, 10",
		"/api/log, false, 15, 10", "/api/plan, false, 15, 10",
		"/api/log, true, -1, 10", "/api/plan, true, -1, 10",
		"/api/log, true, 5, 0", "/api/plan, true, 5, 0",
		"/api/log, true, 5,", "/api/plan, true, 5,"
	})
	void invalidOffsetsReturnJsonBadRequest(String path, boolean publicOnly, int start, String length) throws Exception {
		var request = get(path).param("public", Boolean.toString(publicOnly)).param("start", Integer.toString(start));
		if (length != null) {
			request.param("length", length);
		}
		mvc.perform(request)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.error").isNotEmpty());
	}

	@ParameterizedTest
	@CsvSource({
		"/api/log, 10, 10", "/api/plan, 10, 10",
		"/api/log, 10, 0", "/api/plan, 10, 0",
		"/api/log, 10,", "/api/plan, 10,"
	})
	void alignedOffsetsStillReturnPages(String path, int start, String length) throws Exception {
		when(testInfos.findAllPublicAsSlice(any(Pageable.class)))
			.thenAnswer(call -> new SliceImpl<>(List.of(), call.getArgument(0), false));
		when(plans.findAllPublicAsSlice(any(Pageable.class)))
			.thenAnswer(call -> new SliceImpl<>(List.of(), call.getArgument(0), false));
		var request = get(path).param("public", "true").param("start", Integer.toString(start));
		if (length != null) {
			request.param("length", length);
		}
		mvc.perform(request)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}
}
