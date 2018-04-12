package io.github.martinschneider.yasew.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.codeborne.selenide.WebDriverRunner;

import io.github.martinschneider.yasew.user.UserService;

/**
 * Main configuration class for YASeW
 * 
 * @author Martin Schneider
 */
@Component
public class YasewConfiguration {

	private static final String DEFAULT_BROWSER = "chrome";
	private static final String DEFAULT_FEATURE_PATH = "src/test/resources/features";

	@Value("${web.browser:" + DEFAULT_BROWSER + "}")
	private String browser;

	@Value("${web.baseUrl}")
	private String baseUrl;

	@Value("${features.path:" + DEFAULT_FEATURE_PATH + "}")
	private String featuresPath;

	@Value("${steps.package}")
	private String stepsPackage;

	@Value("${application.name}")
	private String applicationName;

	@Value("${platform}")
	private String platform;

	@Value("${eyes.enabled:false}")
	private boolean eyesEnabled;

	@Value("${galen.enabled:false}")
	private boolean galenEnabled;

	private WebDriverFactory webDriverBuilder;

	private UserService userService;

	@Autowired
	public YasewConfiguration(WebDriverFactory webDriverBuilder, UserService userService) {
		this.webDriverBuilder = webDriverBuilder;
		this.userService = userService;
	}

	/**
	 * initialise the user service
	 */
	@PostConstruct
	public void initCucumberConfig() {
		userService.initialize();
	}

	/**
	 * set the correct {@link org.openqa.selenium.WebDriver}
	 */
	public void initWebDriver() {
		// for web the Selenide default behavior is sufficient
		System.setProperty("browser", browser);
		System.setProperty("chromeoptions.args", "--app=" + baseUrl);

		// for Android and IOS we construct the {@link WebDriver} ourselves
		switch (platform) {
		case Platform.ANDROID:
			WebDriverRunner.setWebDriver(webDriverBuilder.getAndroidDriver());
			break;
		case Platform.IOS:
			WebDriverRunner.setWebDriver(webDriverBuilder.getIOSDriver());
			break;
		}
	}

	/**
	 * @return the base URL of the application under test
	 */
	public String getBaseUrl() {
		if (!getPlatform().equals(Platform.WEB)) {
			throw new UnsupportedOperationException("baseUrl is only available for platform web");
		}
		return baseUrl;
	}

	/**
	 * @return the platform to test against
	 */
	public String getPlatform() {
		return platform;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getFeaturesPath() {
		return featuresPath;
	}

	public boolean isEyesEnabled() {
		return eyesEnabled;
	}

	public boolean isGalenEnabled() {
		return galenEnabled;
	}
}