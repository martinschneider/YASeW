@android @opencv
Feature: New question

Scenario: Verify quick link visibility
	Given I am on the homepage 
	Then I can see the question icon

Scenario: Ask a question using quick link
	Given I am on the homepage 
	When I click on the question icon
	Then I can enter a new question