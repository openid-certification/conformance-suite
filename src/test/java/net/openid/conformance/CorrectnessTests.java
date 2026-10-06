package net.openid.conformance;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CorrectnessTests {

	static {
		Security.addProvider(new BouncyCastleJsseProvider());
	}

	@Test
	public void ensureBCJSSEInitialized() {
		assertTrue(Arrays.stream(Security.getProviders()).anyMatch(p -> p.getName().equals("BCJSSE")), "BCJSSE Security provider should be present");
	}

	@Test
	public void ensureTestPlanNamesAreUnique() {

		Stream<Class<?>> classStream = findTestPlanClasses();
		List<Class<?>> collect = classStream.collect(Collectors.toList());
		Set<String> found = new HashSet<>();
		for(Class<?> clazz: collect) {
			PublishTestPlan publishTestPlan = clazz.getDeclaredAnnotation(PublishTestPlan.class);
			String planName = publishTestPlan.testPlanName();
			if(found.contains(planName)) {
				fail("The test plan %s is not unique - this is not allowed".formatted(planName));
			}
			found.add(planName);
		}

	}

	@Test
	public void ensureTestPlanShortNamesAreSetAndDistinct() {
		Set<String> found = new HashSet<>();
		findTestPlanClasses().forEach(clazz -> {
			PublishTestPlan plan = clazz.getDeclaredAnnotation(PublishTestPlan.class);
			assertFalse(plan.shortName().isBlank(),
				"The test plan %s has no shortName".formatted(plan.testPlanName()));
			// the plan picker shows the version only through the short name
			assertTrue(plan.shortName().startsWith(plan.specVersion()),
				"The test plan %s has specVersion '%s' but its shortName '%s' does not start with it"
					.formatted(plan.testPlanName(), plan.specVersion(), plan.shortName()));
			// inside a family the plan picker names each row by its shortName under an entity heading,
			// so two plans with the same family, entity and shortName would read as the same plan
			String row = String.join("|", plan.specFamily(), pickerEntity(plan.profile()), plan.shortName());
			if (!found.add(row)) {
				fail("The test plan %s has the same shortName '%s' as another plan for the same family and entity"
					.formatted(plan.testPlanName(), plan.shortName()));
			}
		});
	}

	/**
	 * The plan picker's entity heading for a profile. Mirrors ENTITY_LABELS in
	 * static/components/test-selector-grouping.js, which shows both relying-party profiles
	 * under one heading.
	 */
	private static String pickerEntity(String profile) {
		return TestPlan.ProfileNames.rplogouttest.equals(profile) ? TestPlan.ProfileNames.rptest : profile;
	}

	@Test
	public void ensureTestPlanStatusMatchesDisplayName() {
		findTestPlanClasses().forEach(clazz -> {
			PublishTestPlan plan = clazz.getDeclaredAnnotation(PublishTestPlan.class);
			String displayName = plan.displayName().toLowerCase(Locale.ROOT);
			assertEquals(!plan.certifiable(), displayName.contains("certification program"),
				"The test plan %s sets certifiable = %s, which disagrees with whether its displayName says it is not part of the certification program"
					.formatted(plan.testPlanName(), plan.certifiable()));
			assertEquals(plan.alpha(), displayName.contains("alpha"),
				"The test plan %s sets alpha = %s, which disagrees with whether its displayName says it is alpha"
					.formatted(plan.testPlanName(), plan.alpha()));
		});
	}

	private static Stream<Class<?>> findTestPlanClasses() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(PublishTestPlan.class));
		Stream.Builder<Class<?>> builder = Stream.builder();
		try {
			for (BeanDefinition bd : scanner.findCandidateComponents("net.openid")) {
				builder.accept(Class.forName(bd.getBeanClassName()));
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Error loading class", e);
		}
		return builder.build();
	}

}
