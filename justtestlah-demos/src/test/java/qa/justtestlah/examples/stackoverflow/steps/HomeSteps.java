package qa.justtestlah.examples.stackoverflow.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import qa.justtestlah.base.BaseSteps;
import qa.justtestlah.examples.stackoverflow.pages.HomePage;
import qa.justtestlah.examples.stackoverflow.pages.NewQuestionPage;
import qa.justtestlah.examples.stackoverflow.pages.android.AndroidHomePage;

public class HomeSteps extends BaseSteps {
  private HomePage home;
  private NewQuestionPage askQuestion;

  @Given("I am on the homepage")
  public void homepage() {
    home.load();
  }

  @When("I go to the tags page")
  public void goToTags() {
    home.navigateToTagsPage();
  }

  @When("I search for {string}")
  public void search(String query) {
    home.search(query);
  }

  @Then("I can see the question icon")
  public void matchQuestionIcon() {
    // The first assertion would be sufficient. We run some more checks to show-case the template
    // matching.
    assertThat(home.hasImage("QUESTION_ICON")).as("Question icon is visible").isEqualTo(true);
    assertThat(home.hasImage("QUESTION_ICON_BLURRED"))
        .as("Question icon is visible (blurred)")
        .isEqualTo(true);
    assertThat(home.hasImage("QUESTION_ICON_DISTORTED"))
        .as("Question icon is visible (distorted)")
        .isEqualTo(true);
    assertThat(home.hasImage("QUESTION_ICON_ROTATED"))
        .as("Question icon is visible (rotated)")
        .isEqualTo(true);
  }

  @Then("I can't see a Facebook icon")
  public void noFacebook() {
    assertThat(home.hasImage("FACEBOOK_ICON")).isEqualTo(false);
  }

  @When("I click on the question icon")
  public void questionIcon() {
    // this step is platform-dependent
    ((AndroidHomePage) home).tapOnQuestionIcon();
  }

  @Then("I can enter a new question")
  public void checkQuestionPage() {
    assertThat(askQuestion.isDisplayed()).as("check for ask question page").isEqualTo(true);
  }
}
