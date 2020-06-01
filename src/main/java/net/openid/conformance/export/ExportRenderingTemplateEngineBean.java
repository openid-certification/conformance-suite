package net.openid.conformance.export;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.SpringTemplateEngine;
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
		templateResolver.setTemplateMode(TemplateMode.HTML);
		return templateResolver;
	}
}
