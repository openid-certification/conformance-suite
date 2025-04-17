package net.openid.conformance;

import net.openid.conformance.plan.PublishTestPlan;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
