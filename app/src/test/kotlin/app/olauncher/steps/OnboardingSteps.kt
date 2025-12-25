package app.olauncher.steps

import app.olauncher.data.Prefs
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class OnboardingSteps {

    private val prefs = mock(Prefs::class.java)
    private var isDefaultLauncher = false

    @Given("the app is installed for the first time")
    fun givenAppInstalledFirstTime() {
        `when`(prefs.firstOpen).thenReturn(true)
    }

    @Given("I have been using the launcher for at least {int} minutes")
    fun givenLauncherUsedMinutes(minutes: Int) {
        `when`(prefs.firstOpenTime).thenReturn(System.currentTimeMillis() - minutes * 60 * 1000)
    }

    @Given("Olauncher is set as the default launcher")
    fun givenOlauncherIsDefault() {
        isDefaultLauncher = true
    }

    @When("the app checks for messages")
    fun whenAppChecksForMessages() {
        // This method will be implemented later, with the logic that checks for messages
    }

    @Then("I should see the {string} dialog")
    fun thenIShouldSeeDialog(dialogName: String) {
        // This method will be implemented later, to verify the dialog visibility
    }

    @Then("the user state should transition to {string}")
    fun thenUserStateShouldTransitionTo(state: String) {
        `when`(prefs.userState).thenReturn(state)
    }

    @Given("my user state is {string}")
    fun givenMyUserStateIs(state: String) {
        `when`(prefs.userState).thenReturn(state)
    }

    @Given("I have been using the launcher for at least {int} hour")
    fun givenLauncherUsedHour(hour: Int) {
        `when`(prefs.firstOpenTime).thenReturn(System.currentTimeMillis() - hour * 60 * 60 * 1000)
    }

    @Given("at least {int} days have passed since first open")
    fun givenDaysPassedSinceFirstOpen(days: Int) {
        `when`(prefs.firstOpenTime).thenReturn(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000)
    }

    @Given("it is currently after 4:00 PM")
    fun givenItIsAfter4PM() {
        // This method will be implemented later, to check the time
    }

    @Given("at least {int} days have passed since the previous share prompt")
    fun givenDaysPassedSincePreviousSharePrompt(days: Int) {
        `when`(prefs.shareShownTime).thenReturn(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000)
    }
}
