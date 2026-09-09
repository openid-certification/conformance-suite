package net.openid.conformance.statistics;

import net.openid.conformance.plan.PublishTestPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the generated map of retired test plan names current.
 *
 * <p>The map is mined from git history by {@code scripts/generate-legacy-plan-aliases.sh},
 * by hand, and leaves out every name the suite still publishes. A plan retired or renamed
 * after the map was generated therefore has no entry until someone regenerates, and its
 * runs quietly land under "Other / retired" on the statistics page. The generator records
 * the names that were published when it ran; this test diffs them against what is
 * published now, so that retiring a plan fails the build until the map catches up. It
 * needs no git and no history walk: only the two generated files and the registry.
 */
class LegacyPlanFamilies_UnitTest {

	private static final String REGENERATE = "; regenerate the map with scripts/generate-legacy-plan-aliases.sh";

	@Test
	void everyPlanRetiredSinceTheMapWasGeneratedIsInTheMap() throws IOException {
		Set<String> publishedThen = publishedWhenGenerated();
		Set<String> publishedNow = publishedNow();
		Set<String> aliased = aliases().stringPropertyNames();

		Set<String> retiredSince = new TreeSet<>(publishedThen);
		retiredSince.removeAll(publishedNow);
		Set<String> missing = new TreeSet<>(retiredSince);
		missing.removeAll(aliased);
		assertThat(missing)
			.as("test plans retired or renamed since " + SpecFamilyResolver.ALIASES + " was generated, "
				+ "whose runs would be charted under \"Other / retired\"" + REGENERATE)
			.isEmpty();
	}

	@Test
	void noAliasNamesAPlanTheSuitePublishesAgain() throws IOException {
		Set<String> republished = new TreeSet<>(aliases().stringPropertyNames());
		republished.retainAll(publishedNow());
		assertThat(republished)
			.as("aliases naming a plan the registry publishes; they are dead weight the resolver drops" + REGENERATE)
			.isEmpty();
	}

	@Test
	void thePublishedListWasRecordedAlongsideTheMap() throws IOException {
		// a generator that did not record the list would make the first test vacuously green
		assertThat(publishedWhenGenerated()).as("names in " + SpecFamilyResolver.PUBLISHED).isNotEmpty();
	}

	/** @return the plan names the registry publishes now, from every {@link PublishTestPlan} on the classpath */
	private static Set<String> publishedNow() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(PublishTestPlan.class));
		Set<String> names = new TreeSet<>();
		for (BeanDefinition definition : scanner.findCandidateComponents("net.openid")) {
			try {
				names.add(Class.forName(definition.getBeanClassName()).getAnnotation(PublishTestPlan.class).testPlanName());
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("scanner named a class that does not load", e);
			}
		}
		return names;
	}

	/** @return the plan names that were published when the map was generated, as the generator recorded them */
	private static Set<String> publishedWhenGenerated() throws IOException {
		Set<String> names = new TreeSet<>();
		try (InputStream stream = resource(SpecFamilyResolver.PUBLISHED);
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String name = line.strip();
				if (!name.isEmpty() && !name.startsWith("#")) {
					names.add(name);
				}
			}
		}
		return names;
	}

	private static Properties aliases() throws IOException {
		Properties properties = new Properties();
		try (InputStream stream = resource(SpecFamilyResolver.ALIASES)) {
			properties.load(stream);
		}
		return properties;
	}

	private static InputStream resource(String path) {
		InputStream stream = LegacyPlanFamilies_UnitTest.class.getResourceAsStream(path);
		assertThat(stream).as("generated file %s on the classpath", path).isNotNull();
		return stream;
	}
}
