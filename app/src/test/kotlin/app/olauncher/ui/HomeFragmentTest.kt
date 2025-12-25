package app.olauncher.ui

import android.app.Application
import android.os.Build
import androidx.fragment.app.testing.FragmentScenario
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class HomeFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<HomeFragment>
    private val navController = mock(NavController::class.java)
    private val prefs = mock(Prefs::class.java)

    @Before
    fun setUp() {
        // Launch the fragment and inject dependencies
        fragmentScenario = FragmentScenario.launchInContainer(HomeFragment::class.java)
        fragmentScenario.onFragment {
            it.prefs = prefs
            Navigation.setViewNavController(it.requireView(), navController)
        }
    }

    @Test
    fun `onSwipeUp opens the app list`() {
        // When: I swipe up on the screen
        fragmentScenario.onFragment { it.onSwipeUp() }

        // Then: the app list should be displayed
        verify(navController).navigate(
            R.id.action_mainFragment_to_appListFragment,
            ArgumentMatchers.any()
        )
    }

    @Test
    fun `onSwipeDown opens search when action is set to SEARCH`() {
        // Given: the swipe down action is set to "SEARCH"
        `when`(prefs.swipeDownAction).thenReturn(Constants.SwipeDownAction.SEARCH)

        // When: I swipe down on the screen
        fragmentScenario.onFragment { it.onSwipeDown() }

        // Then: the search interface should be opened
        val shadowApp: ShadowApplication = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        Assert.assertNotNull(shadowApp.nextStartedActivity)
    }

    @Test
    fun `onSwipeDown expands notifications when action is set to NOTIFICATIONS`() {
        // Given: the swipe down action is set to "NOTIFICATIONS"
        `when`(prefs.swipeDownAction).thenReturn(Constants.SwipeDownAction.NOTIFICATIONS)

        // When: I swipe down on the screen
        fragmentScenario.onFragment { it.onSwipeDown() }

        // Then: the system notification drawer should be expanded
        val shadowApp: ShadowApplication = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        Assert.assertNull(shadowApp.nextStartedActivity)
    }
}