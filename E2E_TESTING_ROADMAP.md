# End-to-End (E2E) Testing Roadmap with Espresso

This document outlines the strategy for implementing high-fidelity, end-to-end tests using the Espresso framework. These tests will form the top of our "Testing Pyramid," ensuring that critical user journeys work correctly on a real device or emulator.

## Phase 1: Setup and Dependencies

1.  **Add Espresso Dependencies:** Add the necessary Espresso libraries to the `app/build.gradle` file.
    ```groovy
    dependencies {
        // ... other dependencies
        androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
        androidTestImplementation 'androidx.test.ext:junit:1.1.5'
        androidTestImplementation 'androidx.test.espresso:espresso-intents:3.5.1'
        androidTestImplementation 'androidx.test:runner:1.5.2'
        androidTestImplementation 'androidx.test:rules:1.5.0'
    }
    ```

2.  **Configure Test Runner:** Ensure that the `build.gradle` file is configured to use the `AndroidJUnitRunner` for instrumentation tests.
    ```groovy
    android {
        defaultConfig {
            testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    ```

## Phase 2: Implementing Onboarding Flow Tests

The `onboarding_flow.feature` provides the perfect scenarios for our first E2E tests.

### Test Implementation Steps (`OnboardingSteps.kt` in `androidTest` directory):

1.  **Launch Activity:** Use an `ActivityScenarioRule` to launch the `MainActivity` before each test.

    ```kotlin
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)
    ```

2.  **`whenAppChecksForMessages` Step:** This step will need to trigger the logic that you have in your app for checking messages. This might involve advancing a clock or calling a specific method on your `MainViewModel`.

3.  **`thenIShouldSeeDialog` Step:** This will be the core assertion. We will use Espresso's `ViewMatchers` to find the dialog on the screen.

    ```kotlin
    import androidx.test.espresso.Espresso.onView
    import androidx.test.espresso.assertion.ViewAssertions.matches
    import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
    import androidx.test.espresso.matcher.ViewMatchers.withText

    // ...

    onView(withText(dialogName)).check(matches(isDisplayed()))
    ```

## Phase 3: Implementing Home Screen Gesture Tests

These tests from `home_screen_gestures.feature` will confirm that physical interactions with the screen produce the correct results.

### Test Implementation Steps (`HomeScreenSteps.kt` in `androidTest` directory):

1.  **Simulate Gestures:** Use Espresso's `ViewActions` to perform swipes and clicks.

    ```kotlin
    import androidx.test.espresso.action.ViewActions.*

    // For a swipe up
    onView(withId(R.id.home_screen_container)).perform(swipeUp())

    // For a double tap
    onView(withId(R.id.home_screen_container)).perform(doubleClick())
    ```

2.  **Verify Navigation and Intents:**
    *   For navigation within the app (e.g., opening the app list), we will assert that a new view, unique to that screen, is now displayed.
    *   For opening external apps (like the Clock or Calendar), we will use `espresso-intents` to verify that the correct `Intent` was fired.

    ```kotlin
    import androidx.test.espresso.intent.Intents
    import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
    import android.content.Intent

    // ...

    // In the @Before rule
    Intents.init()

    // In the @After rule
    Intents.release()

    // In the "Then" step
    Intents.intended(hasAction(Intent.ACTION_MAIN))
    ```

By following this roadmap, we can build a small but powerful suite of E2E tests that provide the highest level of confidence that our app works as expected for our users.
