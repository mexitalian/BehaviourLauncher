Feature: Onboarding and User Prompts
  As a new Olauncher user
  I want to be guided through the app setup and prompted for feedback at the right time
  So that I can get the most out of the launcher without being overwhelmed

  Background:
    Given the app is installed for the first time

  Scenario: Initial setup triggers wallpaper prompt
    Given I have been using the launcher for at least 10 minutes
    And Olauncher is set as the default launcher
    When the app checks for messages
    Then I should see the "Wallpaper" dialog
    And the user state should transition to "REVIEW"

  Scenario: Review prompt after one hour
    Given my user state is "REVIEW"
    And Olauncher is set as the default launcher
    And I have been using the launcher for at least 1 hour
    When the app checks for messages
    Then I should see the "Review" dialog
    And the user state should transition to "SHARE"

  Scenario: Rating prompt after one week
    Given my user state is "RATE"
    And Olauncher is set as the default launcher
    And at least 7 days have passed since first open
    And it is currently after 4:00 PM
    When the app checks for messages
    Then I should see the "Rate" dialog
    And the user state should transition to "SHARE"

  Scenario: Share prompt after two weeks
    Given my user state is "SHARE"
    And Olauncher is set as the default launcher
    And at least 14 days have passed since first open
    And at least 70 days have passed since the previous share prompt
    And it is currently after 4:00 PM
    When the app checks for messages
    Then I should see the "Share" dialog
