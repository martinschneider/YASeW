package io.github.martinschneider.yasew.configuration;

import com.applitools.eyes.selenium.Eyes;
import com.galenframework.reports.GalenTestInfo;
import io.github.martinschneider.yasew.locator.LocatorParser;
import io.github.martinschneider.yasew.user.UserService;
import io.github.martinschneider.yasew.visual.TemplateMatcher;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.yaml.snakeyaml.Yaml;

/** YASeW Spring context. */
@Configuration
@PropertySource(
  value = {"yasew.properties", "file:${yasew.properties}"},
  ignoreResourceNotFound = true
)
@ComponentScan(basePackages = {"${pages.package}", "${steps.package}"})
public class SpringContext {

  @Value("${eyes.apiKey}")
  private String eyesApiKey;

  @Value("${cloudprovider:local}")
  private String cloudProvider;

  private List<GalenTestInfo> galenTests = new LinkedList<>();

  @Bean
  public YasewConfiguration config() {
    return new YasewConfiguration(webDriverBuilder(), userService());
  }
  
  @Bean
  public Yaml yamlParser()
  {
    return new Yaml();
  }
  
  @Bean
  public LocatorParser locatorParser()
  {
    return new LocatorParser();
  }

  /**
   * Construct the matching {@link WebDriverBuilder}.
   * 
   * @return {@link WebDriverBuilder} matching the configured cloud provider
   */
  @Bean
  public WebDriverBuilder webDriverBuilder() {
    if (cloudProvider.equals("browserstack")) {
      return new BrowserStackWebDriverBuilder();
    }
    return new LocalWebDriverBuilder();
  }

  @Bean
  public TemplateMatcher templateMatcher() {
    return new TemplateMatcher();
  }

  @Bean
  public UserService userService() {
    return new UserService();
  }

  /**
   * Applitools.
   *
   * @return Applitools {@link Eyes}
   */
  @Bean(destroyMethod = "")
  /**
   * Spring would call close() otherwise which will throw an Exception because we already close it
   * ourselves.
   */
  public Eyes eyes() {
    Eyes eyes = new Eyes();
    eyes.setApiKey(eyesApiKey);
    return eyes;
  }

  @Bean
  public List<GalenTestInfo> galenTests() {
    return galenTests;
  }
}
