package net.openid.conformance.config;

import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class ConfigurablePlanSurfacingTests {

	@Test
	public void byDefaultAllTestPlansAreReturned() {

		Collection<VariantService.TestPlanHolder> testPlans = variantService().getTestPlans();

		List<String> profilesToSurface = List.of(
			TestPlan.ProfileNames.optest,
			TestPlan.ProfileNames.rptest,
			TestPlan.ProfileNames.rplogouttest,
			TestPlan.ProfileNames.ekyctest,
			TestPlan.ProfileNames.wallettest,
			TestPlan.ProfileNames.verifierTest,
			TestPlan.ProfileNames.federationTest);
		List<String> profilesSurfaced = profilesPresent(testPlans);

		assertThat(profilesToSurface, containsInAnyOrder(profilesSurfaced.toArray()));

	}

	@Test
	public void onlyDesiredPlansAreSurfaced() {

		Collection<VariantService.TestPlanHolder> testPlans = variantService(TestPlan.ProfileNames.optest).getTestPlans();

		List<String> profilesToSurface = List.of(TestPlan.ProfileNames.optest);
		List<String> profilesSurfaced = profilesPresent(testPlans);

		assertThat(profilesToSurface, containsInAnyOrder(profilesSurfaced.toArray()));

	}

	@Test
	public void canConfigureMiltipleProfiles() {

		Collection<VariantService.TestPlanHolder> testPlans = variantService(TestPlan.ProfileNames.optest, TestPlan.ProfileNames.rptest).getTestPlans();

		List<String> profilesToSurface = List.of(TestPlan.ProfileNames.optest, TestPlan.ProfileNames.rptest);
		List<String> profilesSurfaced = profilesPresent(testPlans);

		assertThat(profilesToSurface, containsInAnyOrder(profilesSurfaced.toArray()));

	}

	private VariantService variantService(String... profiles) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		// Following step normally happens as part of SpringApplication.run
		// needed for correct property conversion
		ctx.getBeanFactory().setConversionService(new DefaultConversionService());

		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("fintechlabs.profiles.visible", String.join(",", profiles));
		ctx.setEnvironment(environment);
		ctx.scan("net.openid.conformance.variant");

		ctx.refresh();
		ctx.start();

		return ctx.getBean(VariantService.class);

	}

	private List<String> profilesPresent(Collection<VariantService.TestPlanHolder> testPlans) {
		return testPlans
			.stream()
			.map(t -> t.info.profile())
			.distinct()
			.collect(Collectors.toList());
	}

}
