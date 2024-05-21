package net.openid.conformance.export;

import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Used for unit tests only
 */
public class ExportRenderingTemplateEngineBean
{
	public SpringTemplateEngine exportRenderingTemplateEngine() {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(exportRenderingTemplateResolver());
		return templateEngine;
	}

	public ClassLoaderTemplateResolver exportRenderingTemplateResolver() {
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		//th:include in test.html will fail without this when running unit tests
		templateResolver.addTemplateAlias("self-contained-export/log-entry.html", "/templates/self-contained-export/log-entry.html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		return templateResolver;
	}
}
