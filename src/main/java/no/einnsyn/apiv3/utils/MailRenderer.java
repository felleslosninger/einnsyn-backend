package no.einnsyn.apiv3.utils;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

@Service
public class MailRenderer {

  Map<String, Mustache> templateCache = new HashMap<>();

  MustacheFactory mustacheFactory = new DefaultMustacheFactory();

  public MailRenderer() {}


  /**
   * Get a cached Mustache instance, or create it if the template hasn't been used before
   * 
   * @param templateName
   * @return
   * @throws Exception
   */
  private Mustache getTemplate(String templateName) throws Exception {
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
   * 
   * @param templateName
   * @param context
   * @return
   * @throws Exception
   */
  public String render(String templateName, Map<String, Object> context) throws Exception {
    // Get correct template for language, and render it
    var template = getTemplate(templateName);
    var writer = new StringWriter();
    return template.execute(writer, context).toString();
  }
}
