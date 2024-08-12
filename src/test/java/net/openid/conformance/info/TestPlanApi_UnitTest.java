package net.openid.conformance.info;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TestPlanApi_UnitTest {

	TestPlanService planService;
	TestInfoService infoService;
	Plan plan;
	TestPlanApi api;

	@BeforeEach
	public void setUp() throws Exception {
		planService = Mockito.mock(TestPlanService.class);
		infoService = Mockito.mock(TestInfoService.class);
		plan = Mockito.mock(Plan.class);
		api = new TestPlanApi();
		ReflectionTestUtils.setField(api, "planService", planService);
		ReflectionTestUtils.setField(api, "infoService", infoService);
	}

	@Test
	public void api_returns_not_found_if_the_test_plan_id_cannot_be_found() {
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(null);

		ResponseEntity<StreamingResponseBody> response = api.deleteMutableTestPlan("abc");

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	public void if_the_test_plan_is_immutable_then_it_cannot_be_deleted() {
		Mockito.when(plan.getImmutable()).thenReturn(true);
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(plan);

		ResponseEntity<StreamingResponseBody> response = api.deleteMutableTestPlan("abc");

		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
	}

	@Test
	public void if_the_test_plan_immutability_property_is_null_then_it_is_mutable_and_can_be_deleted() {
		Mockito.when(plan.getImmutable()).thenReturn(null);
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(plan);

		ResponseEntity<StreamingResponseBody> response = api.deleteMutableTestPlan("abc");

		verify(planService, times(1)).deleteMutableTestPlan("abc");
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void if_the_test_plan_immutability_property_is_null_then_it_is_deleted() {
		Mockito.when(plan.getImmutable()).thenReturn(null);
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(plan);

		api.deleteMutableTestPlan("abc");

		verify(planService, times(1)).deleteMutableTestPlan("abc");
	}

	@Test
	public void deleting_the_test_plan_also_means_that_the_associated_tests_are_deleted() {
		List<String> testIds = new ArrayList<>();
		List<Plan.Module> modules = new ArrayList<>();
		for(int i = 0; i < 6; i += 2) {
			String firstTestId = "testId" + i;
			String secondTestId = "testId" + (i+1);
			testIds.addAll(Arrays.asList(firstTestId, secondTestId));

			Plan.Module module = Mockito.mock(Plan.Module.class);
			Mockito.when(module.getInstances()).thenReturn(Arrays.asList(firstTestId, secondTestId));
			modules.add(module);
		}
		Mockito.when(plan.getModules()).thenReturn(modules);
		Mockito.when(plan.getImmutable()).thenReturn(null);
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(plan);

		api.deleteMutableTestPlan("abc");

		verify(planService, times(1)).deleteMutableTestPlan("abc");
		verify(infoService, times(1)).deleteTests(testIds);
	}

}
