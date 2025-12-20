Feature: Home Screen Gestures
  As an Olauncher user
  I want to navigate my phone using simple gestures
  So that I can access my apps quickly and focus on my tasks

  Background:
    Given I am on the home screen

  Scenario: Swipe up opens the app list
    When I swipe up on the screen
    Then the app list should be displayed

  Scenario: Swipe down for search
    Given the swipe down action is set to "SEARCH"
    When I swipe down on the screen
    Then the search interface should be opened

  Scenario: Swipe down for notifications
    Given the swipe down action is set to "NOTIFICATIONS"
    When I swipe down on the screen
    Then the system notification drawer should expand

  Scenario: Long press for settings
    When I long press on the screen
    Then the settings screen should be opened

  Scenario: Double tap to lock
    Given the "Double tap to lock" feature is enabled
    When I double tap on the screen
    Then the device should be locked

  Scenario: Clicking the clock
    When I click on the clock text
    Then the configured alarm or clock app should open

  Scenario: Clicking the date
    When I click on the date text
    Then the configured calendar app should open
