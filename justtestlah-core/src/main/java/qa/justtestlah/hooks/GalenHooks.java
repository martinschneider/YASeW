package qa.justtestlah.hooks;

import com.galenframework.reports.GalenTestInfo;
import com.galenframework.reports.HtmlReportBuilder;
import io.cucumber.core.api.Scenario;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import qa.justtestlah.configuration.JustTestLahConfiguration;

/** Galen hooks. */
@Component
public class GalenHooks extends AbstractCucumberHook {

  private static final String GALEN_REPORT_FOLDER_DATE_PATTERN = "yyyy-MM-dd HH.mm.ss";

  private static final Logger LOG = LoggerFactory.getLogger(GalenHooks.class);

  @Autowired private JustTestLahConfiguration configuration;

  @Autowired private List<GalenTestInfo> galenTests;

  /**
   * Generate Galen reports.
   *
   * @param scenario Cucumber scenario
   */
  public void after(Scenario scenario) {
    if (configuration.isGalenEnabled()) {
      LOG.info("Generating {} Galen reports", galenTests.size());
      try {
        new HtmlReportBuilder().build(galenTests, getGalenReportDirectory());
      } catch (IOException exception) {
        throw new RuntimeException("Error generating Galen reports.", exception);
      }
    }
  }

  private String getGalenReportDirectory() {
    return configuration.getGalenReportDirectory()
        + "/"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern(GALEN_REPORT_FOLDER_DATE_PATTERN));
  }
}
