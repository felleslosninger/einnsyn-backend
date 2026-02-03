package no.einnsyn.backend.utils.mail;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MailRendererService {

  Map<String, Mustache> templateCache = new HashMap<>();

  MustacheFactory mustacheFactory = new DefaultMustacheFactory();

  /**
   * Get a cached Mustache instance, or create it if the template hasn't been used before.
   *
   * @param templateName the name of the template
   * @return the Mustache template instance
   */
  private Mustache getTemplate(String templateName) {
    if (templateCache.containsKey(templateName)) {
      return templateCache.get(templateName);
    } else {
      Mustache template = null;
      template = mustacheFactory.compile(templateName);
      templateCache.put(templateName, template);
      return template;
    }
  }

  /**
   * Renders a template file with the given context.
   *
   * @param templateName the name of the template file
   * @param context the context variables for the template
   * @return the rendered template as a string
   */
  public String renderFile(String templateName, Map<String, Object> context) {
    // Get correct template for language, and render it
    var template = getTemplate(templateName);
    var writer = new StringWriter();
    return template.execute(writer, context).toString();
  }
}
