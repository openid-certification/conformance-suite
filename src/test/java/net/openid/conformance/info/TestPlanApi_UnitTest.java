package net.openid.conformance.info;

import net.openid.conformance.runner.TestRunnerSupport;
import org.springframework.data.mongodb.core.MongoTemplate;
import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TestPlanApi_UnitTest {

	TestPlanService planService;
	TestInfoService infoService;
	TestRunnerSupport testRunnerSupport;
	Plan plan;
	TestPlanApi api;

	@BeforeEach
	public void setUp() throws Exception {
		planService = Mockito.mock(TestPlanService.class);
		infoService = Mockito.mock(TestInfoService.class);
		testRunnerSupport = Mockito.mock(TestRunnerSupport.class);
		plan = Mockito.mock(Plan.class);
		api = new TestPlanApi();
		ReflectionTestUtils.setField(api, "planService", planService);
		ReflectionTestUtils.setField(api, "infoService", infoService);
		// stopping a running module before its rows go is the bulk deleter's method now, so that
		// one delete and many deletes cannot drift apart; a real one is wired around the same
		// mocks, and the ordering assertion below is unchanged by that
		ReflectionTestUtils.setField(api, "bulkPlanDeleter",
			new BulkPlanDeleter(Mockito.mock(MongoTemplate.class), infoService, testRunnerSupport));
	}

	@Test
	public void api_returns_not_found_if_the_test_plan_id_cannot_be_found() {
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(null);

		ResponseEntity<Void> response = api.deleteMutableTestPlan("abc");

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	public void if_the_test_plan_is_immutable_then_it_cannot_be_deleted() {
		Mockito.when(plan.getImmutable()).thenReturn(true);
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(plan);

		ResponseEntity<Void> response = api.deleteMutableTestPlan("abc");

		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
	}

	@Test
	public void if_the_test_plan_immutability_property_is_null_then_it_is_mutable_and_can_be_deleted() {
		Mockito.when(plan.getImmutable()).thenReturn(null);
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(plan);

		ResponseEntity<Void> response = api.deleteMutableTestPlan("abc");

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

	@Test
	public void a_running_test_is_stopped_before_its_log_entries_are_deleted() {
		List<String> testIds = Arrays.asList("testId0");
		Plan.Module module = Mockito.mock(Plan.Module.class);
		Mockito.when(module.getInstances()).thenReturn(testIds);
		Mockito.when(plan.getModules()).thenReturn(Arrays.asList(module));
		Mockito.when(plan.getImmutable()).thenReturn(null);
		Mockito.when(planService.getTestPlan(anyString())).thenReturn(plan);

		TestModule runningTest = Mockito.mock(TestModule.class);
		Mockito.when(testRunnerSupport.getRunningTestById("testId0")).thenReturn(runningTest);

		api.deleteMutableTestPlan("abc");

		// stop() writes the module's final log entries, so it has to run before they are deleted
		InOrder inOrder = Mockito.inOrder(runningTest, infoService);
		inOrder.verify(runningTest).stop(anyString());
		inOrder.verify(infoService).deleteTests(testIds);
	}

	/** The shape {@code TestPlanHolder.getVariantSummary()} answers with. */
	private static Map<String, Object> variantSummary(String parameter, String... values) {
		Map<String, Object> allowed = new LinkedHashMap<>();
		for (String value : values) {
			allowed.put(value, Map.of("configurationFields", List.of(), "hidesConfigurationFields", List.of()));
		}
		return Map.of(parameter, Map.of(
			"variantInfo", Map.of("displayName", "Client Authentication Type", "description", "..."),
			"variantValues", allowed));
	}

	/**
	 * What the filter controls are offered: the value names alone, out of a summary that also
	 * carries display names, descriptions and the configuration fields each value shows or hides.
	 */
	@Test
	public void variant_values_takes_the_value_names_and_nothing_else() {
		assertThat(TestPlanApi.variantValues(variantSummary("client_auth_type", "private_key_jwt", "mtls")))
			.isEqualTo(Map.of("client_auth_type", List.of("mtls", "private_key_jwt")));
	}

	@Test
	public void variant_values_sorts_both_the_parameters_and_their_values() {
		Map<String, Object> both = new LinkedHashMap<>();
		both.putAll(variantSummary("client_auth_type", "private_key_jwt", "mtls"));
		both.putAll(variantSummary("ciba_mode", "poll", "ping"));

		assertThat(TestPlanApi.variantValues(both).keySet())
			.containsExactly("ciba_mode", "client_auth_type");
		assertThat(TestPlanApi.variantValues(both).get("ciba_mode")).containsExactly("ping", "poll");
	}

	/**
	 * It walks a structure it is handed as {@code Object}, so a change to that shape must leave a
	 * filter control with no options rather than an endpoint that fails.
	 */
	@Test
	public void variant_values_yields_no_options_when_the_summary_is_not_the_expected_shape() {
		assertThat(TestPlanApi.variantValues(null)).isEmpty();
		assertThat(TestPlanApi.variantValues("not a map")).isEmpty();
		assertThat(TestPlanApi.variantValues(Map.of("client_auth_type", "not a map either"))).isEmpty();
		assertThat(TestPlanApi.variantValues(Map.of("client_auth_type", Map.of("variantInfo", Map.of()))))
			.isEmpty();
	}

	/** An account named the only way one can be: both halves of it. */
	private static final PlanOwner ASKED = new PlanOwner("developer", "https://developer.com");

	private static final PlanListFilter NOTHING_ASKED_FOR =
		new PlanListFilter(null, Map.of(), null, null, null, null);

	/**
	 * The rule that stops "delete everything" being expressible. A search narrows a listing as
	 * much as any filter does - the page offers to move the search box's term into the listing
	 * precisely so that what it finds can be deleted - so leaving it out of this made
	 * /plans.html?search=ciba offer a delete button whose request was then refused.
	 */
	@Test
	public void a_search_on_its_own_is_enough_of_a_filter_to_delete_by() {
		assertThat(TestPlanApi.refuseUnusableBulkRequest(NOTHING_ASKED_FOR, null, "\"ciba\"", null)).isNull();
	}

	@Test
	public void an_owner_or_a_filter_on_its_own_is_enough_too() {
		assertThat(TestPlanApi.refuseUnusableBulkRequest(NOTHING_ASKED_FOR, ASKED, null, null)).isNull();
		assertThat(TestPlanApi.refuseUnusableBulkRequest(
			new PlanListFilter(Set.of("a-plan"), Map.of(), null, null, null, null), null, null, null)).isNull();
	}

	@Test
	public void asking_to_delete_with_nothing_narrowing_it_is_refused() {
		ResponseEntity<Object> refusal =
			TestPlanApi.refuseUnusableBulkRequest(NOTHING_ASKED_FOR, null, null, null);

		assertThat(refusal).isNotNull();
		assertThat(refusal.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(String.valueOf(refusal.getBody())).contains("needs a filter").contains("search");
	}

	@Test
	public void a_limit_that_could_delete_nothing_is_refused() {
		assertThat(TestPlanApi.refuseUnusableBulkRequest(NOTHING_ASKED_FOR, ASKED, null, 0))
			.isNotNull();
		assertThat(TestPlanApi.refuseUnusableBulkRequest(NOTHING_ASKED_FOR, ASKED, null, -1))
			.isNotNull();
		assertThat(TestPlanApi.refuseUnusableBulkRequest(NOTHING_ASKED_FOR, ASKED, null, 1))
			.isNull();
	}

}
