package io.fintechlabs.testframework.info;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.fintechlabs.testframework.testmodule.OIDFJSON;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.pagination.PaginationRequest;
import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;
import io.fintechlabs.testframework.testmodule.DataUtils;

@Controller
public class TestPlanApi implements DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(TestPlanApi.class);

	private Supplier<Map<String, TestPlanHolder>> testPlanSupplier = Suppliers.memoize(this::findTestPlans);

	@Autowired
	private TestPlanService planService;

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@PostMapping(value = "/plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> createTestPlan(@RequestParam("planName") String planName, @RequestBody JsonObject config, Model m) {

		String id = RandomStringUtils.randomAlphanumeric(13);

		TestPlanHolder holder = getTestPlans().get(planName);

		if (holder == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String description = null;
		if (config.has("description") && config.get("description").isJsonPrimitive()) {
			description = OIDFJSON.getString(config.get("description"));
		}

		// extract the `publish` field if available
		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		}

		// save the configuration for the test plan
		savedConfigurationService.savePlanConfigurationForCurrentUser(config, planName);

		planService.createTestPlan(id, planName, config, description, holder.testModuleNames, holder.a.summary(), publish);

		Map<String, Object> map = new HashMap<>();
		map.put("name", planName);
		map.put("id", id);
		map.put("modules", holder.testModuleNames);

		return new ResponseEntity<>(map, HttpStatus.CREATED);
	}

	@GetMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getTestPlansForCurrentUser(PaginationRequest page) {

		Map response = planService.getPaginatedPlansForCurrentUser(page);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getTestPlan(@PathVariable("id") String id) {

		Map testPlan = planService.getTestPlan(id);

		if (testPlan != null) {
			return new ResponseEntity<>(testPlan, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

	}

	@PostMapping(value = "/plan/{id}/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> publishTestPlan(@PathVariable("id") String id, @RequestBody JsonObject config) {

		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		} else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (!planService.publishTestPlan(id, publish)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("publish", publish);

		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@GetMapping(value = "plan/info/{planName}")
	public ResponseEntity<Object> getTestPlanInfo(@PathVariable("planName") String planName) {
		TestPlanHolder holder = getTestPlans().get(planName);

		if (holder != null) {

				Map map = args(
					"planName", holder.a.testPlanName(),
					"displayName", holder.a.displayName(),
					"profile", holder.a.profile(),
					"moduleNames", holder.testModuleNames,
					"configurationFields", holder.a.configurationFields(),
					"summary", holder.a.summary());

			return new ResponseEntity<>(map, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping(value = "plan/available")
	public ResponseEntity<Object> getAvailableTestPlans() {
		Set<Map<String, ?>> available = getTestPlans()
			.values().stream()
			.map(e -> args(
				"planName", e.a.testPlanName(),
				"displayName", e.a.displayName(),
				"profile", e.a.profile(),
				"moduleNames", e.testModuleNames,
				"configurationFields", e.a.configurationFields(),
				"summary", e.a.summary()))
			.collect(Collectors.toSet());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}

	private Map<String, TestPlanHolder> getTestPlans() {
		return testPlanSupplier.get();
	}

	private Map<String, TestPlanHolder> findTestPlans() {
		Map<String, TestPlanHolder> testPlans = new HashMap<>();

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(PublishTestPlan.class));
		for (BeanDefinition bd : scanner.findCandidateComponents("io.fintechlabs")) {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends TestPlan> c = (Class<? extends TestPlan>) Class.forName(bd.getBeanClassName());
				PublishTestPlan a = c.getDeclaredAnnotation(PublishTestPlan.class);

				testPlans.put(a.testPlanName(), new TestPlanHolder(c, a));

			} catch (ClassNotFoundException e) {
				logger.error("Couldn't load test module definition: " + bd.getBeanClassName());
			}
		}

		return testPlans;

	}

	private class TestPlanHolder {
		public Class<? extends TestPlan> c;
		public PublishTestPlan a;
		public String[] testModuleNames;

		// TODO: remove use of deprecated testModuleNames
		@SuppressWarnings("deprecation")
		public TestPlanHolder(Class<? extends TestPlan> c, PublishTestPlan a) {
			this.c = c;
			this.a = a;

			if (a.testModules().length > 0) {
				this.testModuleNames = Arrays.stream(a.testModules())
					.map((m) -> m.getDeclaredAnnotation(PublishTestModule.class).testName())
					.toArray(String[]::new);
			} else {
				// TODO: remove use of deprecated testModuleNames; this falls back to the old path is the testModules
				// annotation isn't present
				this.testModuleNames = a.testModuleNames();
			}
		}


	}


	@GetMapping(value = "/public/api/plan")
	public ResponseEntity<Object> getPublicPlans(PaginationRequest page) {

		Map response = planService.getPaginatedPublicPlans(page);

		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@GetMapping(value = "/public/api/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getPublicPlan(@PathVariable("id") String id) {

		Map testPlan = planService.getPublicPlan(id);

		if (testPlan != null) {
			return new ResponseEntity<>(testPlan, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

	}

}
