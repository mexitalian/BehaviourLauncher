package app.olauncher.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.FolderStore
import app.olauncher.data.Member
import app.olauncher.data.Prefs
import app.olauncher.data.VerbFolder
import app.olauncher.databinding.FragmentHomeBinding
import app.olauncher.helper.appUsagePermissionGranted
import app.olauncher.helper.dpToPx
import app.olauncher.helper.expandNotificationDrawer
import app.olauncher.helper.getChangedAppTheme
import app.olauncher.helper.getUserHandleFromString
import app.olauncher.helper.isPackageInstalled
import app.olauncher.helper.openAlarmApp
import app.olauncher.helper.openCalendar
import app.olauncher.helper.openCameraApp
import app.olauncher.helper.openDialerApp
import app.olauncher.helper.setPlainWallpaperByTheme
import app.olauncher.helper.showToast
import app.olauncher.listener.OnSwipeTouchListener
import app.olauncher.listener.ViewSwipeTouchListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var folderStore: FolderStore
    private var expandedFolderId: Long? = null
    private var actionMenuVisible = false
    private var confirmDeleteVisible = false
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        folderStore = FolderStore(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        viewModel.isOlauncherDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            // Home button for recents feature disabled
            // R.id.recents -> {}
            R.id.clock -> openClockApp()
            R.id.date -> openCalendarApp()
            R.id.setDefaultLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.tvScreenTime -> openScreenTimeDigitalWellbeing()

            else -> {
                val tag = view.tag?.toString() ?: ""
                when {
                    tag.startsWith(FAPP_TAG) -> {
                        val parts = tag.removePrefix(FAPP_TAG).split(":", limit = 2)
                        if (parts.size == 2)
                            folderAppClicked(parts[0].toLongOrNull() ?: 0L, parts[1])
                    }

                    tag.startsWith(FOLDER_TAG) -> folderClicked(tag.removePrefix(FOLDER_TAG))

                    tag == EMPTY_TAG -> emptyIntentionClicked()

                    else -> {
                        try { // Launch app
                            val appLocation = tag.toInt()
                            homeAppClicked(appLocation)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank())
            openAlarmApp(requireContext())
        else
            launchApp(
                "Clock",
                prefs.clockAppPackage,
                prefs.clockAppClassName,
                prefs.clockAppUser
            )
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank())
            openCalendar(requireContext())
        else
            launchApp(
                "Calendar",
                prefs.calendarAppPackage,
                prefs.calendarAppClassName,
                prefs.calendarAppUser
            )
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.clock -> {
                showAppList(Constants.FLAG_SET_CLOCK_APP)
                prefs.clockAppPackage = ""
                prefs.clockAppClassName = ""
                prefs.clockAppUser = ""
            }

            R.id.date -> {
                showAppList(Constants.FLAG_SET_CALENDAR_APP)
                prefs.calendarAppPackage = ""
                prefs.calendarAppClassName = ""
                prefs.calendarAppUser = ""
            }

            R.id.tvScreenTime -> {
                showAppList(Constants.FLAG_SET_SCREEN_TIME_APP)
                prefs.screenTimeAppPackage = ""
                prefs.screenTimeAppClassName = ""
                prefs.screenTimeAppUser = ""
            }

            R.id.setDefaultLauncher -> {
                prefs.hideSetDefaultLauncher = true
                binding.setDefaultLauncher.visibility = View.GONE
                if (viewModel.isOlauncherDefault.value != true) {
                    requireContext().showToast(R.string.set_as_default_launcher)
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                }
            }
            else -> {
                val tag = view.tag?.toString() ?: ""
                val folderId =
                    if (tag.startsWith(FOLDER_TAG)) tag.removePrefix(FOLDER_TAG).toLongOrNull() else null
                when {
                    folderId != null -> renameIntention(folderId)
                    tag == EMPTY_TAG -> createIntention()
                    else -> if (tag.startsWith(FAPP_TAG)) {
                        val parts = tag.removePrefix(FAPP_TAG).split(":", limit = 2)
                        if (parts.size == 2)
                            showAppList(
                                Constants.FLAG_SET_FOLDER_APP,
                                rename = true,
                                includeHiddenApps = true,
                                folderId = parts[0].toLongOrNull() ?: 0L,
                                memberPackage = parts[1]
                            )
                    }
                }
            }
        }
        return true
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        } else binding.firstRunTips.visibility = View.GONE

        viewModel.refreshHome.observe(viewLifecycleOwner) {
            populateHomeScreen(it)
        }
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                if (prefs.dailyWallpaper && prefs.appTheme == AppCompatDelegate.MODE_NIGHT_YES) {
                    prefs.dailyWallpaper = false
                    viewModel.cancelWallpaperWorker()
                }
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (binding.firstRunTips.isVisible) return@Observer
            binding.setDefaultLauncher.isVisible = it.not() && prefs.hideSetDefaultLauncher.not()
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            setHomeAlignment(it)
        }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) {
            populateDateTime()
        }
        viewModel.screenTimeValue.observe(viewLifecycleOwner) {
            it?.let { binding.tvScreenTime.text = it }
        }
        // Home button for recents feature disabled
        // viewModel.showRecentApps.observe(viewLifecycleOwner) {
        //     binding.recents.performClick()
        // }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp8))
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        // Home button for recents feature disabled
        // binding.recents.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.clock.setOnLongClickListener(this)
        binding.date.setOnLongClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
        binding.setDefaultLauncher.setOnLongClickListener(this)
        binding.tvScreenTime.setOnClickListener(this)
        binding.tvScreenTime.setOnLongClickListener(this)

        // These fire only on d-pad/keyboard events; touch is consumed by ViewSwipeTouchListener
        binding.homeApp1.setOnClickListener(this)
        binding.homeApp2.setOnClickListener(this)
        binding.homeApp3.setOnClickListener(this)
        binding.homeApp4.setOnClickListener(this)
        binding.homeApp5.setOnClickListener(this)
        binding.homeApp6.setOnClickListener(this)
        binding.homeApp7.setOnClickListener(this)
        binding.homeApp8.setOnClickListener(this)
        binding.homeApp1.setOnLongClickListener(this)
        binding.homeApp2.setOnLongClickListener(this)
        binding.homeApp3.setOnLongClickListener(this)
        binding.homeApp4.setOnLongClickListener(this)
        binding.homeApp5.setOnLongClickListener(this)
        binding.homeApp6.setOnLongClickListener(this)
        binding.homeApp7.setOnLongClickListener(this)
        binding.homeApp8.setOnLongClickListener(this)

        binding.expandedFolderName.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onClick() = exitIntentionView()
            override fun onLongClick() = toggleFolderActions()
            override fun onSwipeDown() = exitIntentionView()
        })
        binding.actionWord1.setOnClickListener { onFolderAction(1) }
        binding.actionWord2.setOnClickListener { onFolderAction(2) }
        binding.actionWord3.setOnClickListener { onFolderAction(3) }
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        val verticalGravity = if (prefs.homeBottomAlignment) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = horizontalGravity or verticalGravity
        binding.dateTimeLayout.gravity = horizontalGravity
        binding.homeApp1.gravity = horizontalGravity
        binding.homeApp2.gravity = horizontalGravity
        binding.homeApp3.gravity = horizontalGravity
        binding.homeApp4.gravity = horizontalGravity
        binding.homeApp5.gravity = horizontalGravity
        binding.homeApp6.gravity = horizontalGravity
        binding.homeApp7.gravity = horizontalGravity
        binding.homeApp8.gravity = horizontalGravity
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)

//        var dateText = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        var dateText = dateFormat.format(Date())

        if (!prefs.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (battery > 0)
                dateText = getString(R.string.day_battery, dateText, battery)
        }
        binding.date.text = dateText.replace(".,", ",")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.getTodaysScreenTime()
        binding.tvScreenTime.visibility = View.VISIBLE

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin = if (isLandscape) 64.dpToPx() else 10.dpToPx()
        val marginTop = if (isLandscape) {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 36.dpToPx() else 56.dpToPx()
        } else {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 45.dpToPx() else 72.dpToPx()
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = marginTop
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
            gravity = if (prefs.homeAlignment == Gravity.END) Gravity.START else Gravity.END
        }
        binding.tvScreenTime.layoutParams = params
        binding.tvScreenTime.setPadding(10.dpToPx())
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            populateScreenTime()

        populateFolders()
        populateExpandedHeader()
    }

    private fun setHomeAppText(
        textView: TextView,
        appName: String,
        packageName: String,
        userString: String,
        isShortcut: Boolean,
        shortcutId: String?,
    ): Boolean {
        // Get user handle for the app/shortcut
        val userHandle = getUserHandleFromString(requireContext(), userString)

        // If it's a shortcut, verify it still exists
        if (isShortcut) {
            val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            // Query for the specific shortcut
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }

            try {
                val shortcuts = launcherApps.getShortcuts(query, userHandle)
                // Check if our shortcut still exists
                if (shortcuts?.any { it.id == shortcutId } == true) {
                    textView.text = appName
                    return true
                }
                textView.text = ""
                return false
            } catch (e: Exception) {
                e.printStackTrace()
                textView.text = ""
                return false
            }
        }

        // Regular app check
        if (isPackageInstalled(requireContext(), packageName, userString)) {
            textView.text = appName
            return true
        }
        textView.text = ""
        return false
    }

    private fun hideHomeApps() {
        binding.homeApp1.visibility = View.GONE
        binding.homeApp2.visibility = View.GONE
        binding.homeApp3.visibility = View.GONE
        binding.homeApp4.visibility = View.GONE
        binding.homeApp5.visibility = View.GONE
        binding.homeApp6.visibility = View.GONE
        binding.homeApp7.visibility = View.GONE
        binding.homeApp8.visibility = View.GONE
    }

    private fun launchAppOrShortcut(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
        fallback: (() -> Unit)? = null,
    ) {
        if (appName.isEmpty()) {
            showLongPressToast()
            return
        }
        if (isShortcut && !shortcutId.isNullOrEmpty()) {
            launchShortcut(
                packageName = packageName,
                shortcutId = shortcutId,
                shortcutLabel = appName,
                userString = userString
            )
        } else if (packageName.isNotEmpty()) {
            launchApp(
                appName = appName,
                packageName = packageName,
                activityClassName = activityClassName,
                userString = userString
            )
        } else {
            fallback?.invoke()
        }
    }

    private fun launchShortcut(shortcutId: String, packageName: String, shortcutLabel: String, userString: String) {
        viewModel.selectedApp(
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = shortcutLabel,
                user = getUserHandleFromString(requireContext(), userString),
                key = null,
                appPackage = packageName,
                isNew = false,
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String) {
        viewModel.selectedApp(
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = packageName,
                activityClassName = activityClassName,
                isNew = false,
                user = getUserHandleFromString(requireContext(), userString)
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun homeAppClicked(location: Int) {
        launchAppOrShortcut(
            appName = prefs.getAppName(location),
            packageName = prefs.getAppPackage(location),
            activityClassName = prefs.getAppActivityClassName(location),
            shortcutId = prefs.getShortcutId(location),
            isShortcut = prefs.getIsShortcut(location),
            userString = prefs.getAppUser(location)
        )
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeRightEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeRight,
            packageName = prefs.appPackageSwipeRight,
            activityClassName = prefs.appActivityClassNameRight,
            shortcutId = prefs.shortcutIdSwipeRight,
            isShortcut = prefs.isShortcutSwipeRight,
            userString = prefs.appUserSwipeRight,
            fallback = { openDialerApp(requireContext()) }
        )
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeLeft,
            packageName = prefs.appPackageSwipeLeft,
            activityClassName = prefs.appActivityClassNameSwipeLeft,
            shortcutId = prefs.shortcutIdSwipeLeft,
            isShortcut = prefs.isShortcutSwipeLeft,
            userString = prefs.appUserSwipeLeft,
            fallback = { openCameraApp(requireContext()) }
        )
    }

    private fun showAppList(
        flag: Int,
        rename: Boolean = false,
        includeHiddenApps: Boolean = false,
        folderId: Long = 0L,
        initialQuery: String? = null,
        memberPackage: String? = null,
    ) {
        viewModel.getAppList(includeHiddenApps)
        val args = bundleOf(
            Constants.Key.FLAG to flag,
            Constants.Key.RENAME to rename
        )
        if (folderId > 0)
            args.putLong(Constants.Key.FOLDER_ID, folderId)
        if (!initialQuery.isNullOrEmpty())
            args.putString(Constants.Key.INITIAL_QUERY, initialQuery)
        if (!memberPackage.isNullOrEmpty())
            args.putString(Constants.Key.MEMBER_PACKAGE, memberPackage)
        try {
            findNavController().navigate(R.id.action_mainFragment_to_appListFragment, args)
        } catch (e: Exception) {
            findNavController().navigate(R.id.appListFragment, args)
            e.printStackTrace()
        }
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun changeAppTheme() {
        if (prefs.dailyWallpaper.not()) return
        val changedAppTheme = getChangedAppTheme(requireContext(), prefs.appTheme)
        prefs.appTheme = changedAppTheme
        if (prefs.dailyWallpaper) {
            setPlainWallpaperByTheme(requireContext(), changedAppTheme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun openScreenTimeDigitalWellbeing() {
        if (prefs.screenTimeAppPackage.isNotBlank()) {
            launchApp(
                "Screen Time",
                prefs.screenTimeAppPackage,
                prefs.screenTimeAppClassName,
                prefs.screenTimeAppUser
            )
            return
        }
        val intent = Intent()
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_ACTIVITY
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                intent.setClassName(
                    Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                    Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLongPressToast() = requireContext().showToast(getString(R.string.long_press_to_select_app))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                if (expandedFolderId != null)
                    exitIntentionView()
                else
                    expandNotificationDrawer(requireContext())
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (!prefs.lockModeOn) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    binding.lock.performClick()
                else
                    lockPhone()
            }

            override fun onClick() {
                super.onClick()
                viewModel.checkForMessages.call()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                if (!onFolderRowSwipeUp(view))
                    showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                if (!onFolderRowSwipeDown(view))
                    expandNotificationDrawer(requireContext())
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    private fun populateFolders() {
        val views = listOf(
            binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
            binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8
        )
        val folders = folderStore.folders
        val expanded = expandedFolderId?.let { id -> folders.firstOrNull { it.id == id } }

        val rows = mutableListOf<HomeRow>()
        if (expanded != null) {
            expanded.members.forEach { rows.add(HomeRow.AppRow(expanded.id, it)) }
        } else {
            val slots = prefs.homeAppsNum.coerceIn(0, views.size)
            folders.take(slots).forEach { rows.add(HomeRow.FolderRow(it)) }
            repeat(slots - rows.size) { rows.add(HomeRow.EmptyRow) }
        }

        views.forEachIndexed { index, view ->
            when (val row = rows.getOrNull(index)) {
                is HomeRow.FolderRow -> {
                    if (row.folder.verb.isBlank()) {
                        view.text = ""
                        view.hint = getString(R.string.intent_placeholder)
                    } else {
                        view.text = row.folder.verb
                        view.hint = ""
                    }
                    view.tag = FOLDER_TAG + row.folder.id
                    view.visibility = View.VISIBLE
                }

                is HomeRow.AppRow -> {
                    view.text = row.member.label ?: appLabelFor(row.member.packageName)
                    view.hint = ""
                    view.tag = FAPP_TAG + row.folderId + ":" + row.member.packageName
                    view.visibility = View.VISIBLE
                }

                is HomeRow.EmptyRow -> {
                    view.text = ""
                    view.hint = getString(R.string.intent_placeholder)
                    view.tag = EMPTY_TAG
                    view.visibility = View.VISIBLE
                }

                null -> {
                    view.visibility = View.GONE
                    view.text = ""
                    view.tag = null
                }
            }
        }
    }

    private fun appLabelFor(packageName: String): String = try {
        val pm = requireContext().packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        packageName
    }

    private fun folderClicked(folderId: String) {
        val folder = folderStore.folders.firstOrNull { it.id.toString() == folderId } ?: return
        if (expandedFolderId == folder.id) {
            exitIntentionView()
            return
        }
        if (folder.members.isEmpty()) {
            enterIntentionView(folder.id)
            return
        }
        launchFolderApp(folder.tapApp ?: return)
    }

    private fun emptyIntentionClicked() {
        val folder = folderStore.createFolder("")
        enterIntentionView(folder.id)
    }

    private fun folderAppClicked(folderId: Long, packageName: String) {
        if (folderId <= 0) return
        launchFolderApp(packageName)
    }

    /** Enter the folder view; toast when there is no primary app yet. */
    private fun enterIntentionView(folderId: Long) {
        expandedFolderId = folderId
        actionMenuVisible = false
        confirmDeleteVisible = false
        val folder = folderStore.folders.firstOrNull { it.id == folderId }
        if (folder == null || folder.members.isEmpty())
            requireContext().showToast(getString(R.string.add_primary_app_hint))
        populateDateTime()
        populateFolders()
        populateExpandedHeader()
    }

    private fun exitIntentionView() {
        expandedFolderId = null
        actionMenuVisible = false
        confirmDeleteVisible = false
        folderStore.cleanupEmptyUnnamed()
        populateDateTime()
        populateFolders()
        populateExpandedHeader()
    }

    /** Swipe up on an intention opens its folder view. */
    private fun onFolderRowSwipeUp(view: View): Boolean {
        val tag = view.tag?.toString() ?: return false
        if (!tag.startsWith(FOLDER_TAG)) return false
        val id = tag.removePrefix(FOLDER_TAG).toLongOrNull() ?: return false
        if (expandedFolderId != id)
            enterIntentionView(id)
        return true
    }

    /** Swipe down anywhere in the folder view exits it. */
    private fun onFolderRowSwipeDown(view: View): Boolean {
        val tag = view.tag?.toString() ?: return false
        if (expandedFolderId == null) return false
        if (tag.startsWith(FOLDER_TAG) || tag.startsWith(FAPP_TAG)) {
            exitIntentionView()
            return true
        }
        return false
    }

    private fun renameIntention(folderId: Long) {
        val current = folderStore.folders.firstOrNull { it.id == folderId }
        showAppList(
            Constants.FLAG_RENAME_INTENTION,
            rename = true,
            folderId = folderId,
            initialQuery = current?.verb ?: ""
        )
    }

    private fun createIntention() {
        showAppList(
            Constants.FLAG_RENAME_INTENTION,
            rename = true,
            folderId = 0L,
            initialQuery = ""
        )
    }

    /** The folder view header: intention name in the clock position, inline actions beneath. */
    private fun populateExpandedHeader() {
        val expanded = expandedFolderId?.let { id -> folderStore.folders.firstOrNull { it.id == id } }
        if (expanded == null) {
            actionMenuVisible = false
            confirmDeleteVisible = false
            binding.expandedFolderName.isVisible = false
            binding.folderActionLine.isVisible = false
            return
        }

        binding.dateTimeLayout.isVisible = true
        binding.clock.isVisible = false
        binding.date.isVisible = false

        if (expanded.verb.isBlank()) {
            binding.expandedFolderName.text = ""
            binding.expandedFolderName.hint = getString(R.string.intent_placeholder)
        } else {
            binding.expandedFolderName.text = expanded.verb
            binding.expandedFolderName.hint = ""
        }
        binding.expandedFolderName.isVisible = true

        binding.folderActionLine.isVisible = actionMenuVisible
        if (confirmDeleteVisible) {
            binding.actionWord1.text = getString(R.string.action_yes_delete)
            binding.actionWord2.text = getString(R.string.action_no_cancel)
            binding.actionWord3.isVisible = false
            binding.actionDivider2.isVisible = false
        } else {
            binding.actionWord1.text = getString(R.string.action_add_app)
            binding.actionWord2.text = getString(R.string.action_rename)
            binding.actionWord3.text = getString(R.string.action_delete)
            binding.actionWord3.isVisible = true
            binding.actionDivider2.isVisible = true
        }
    }

    private fun toggleFolderActions() {
        if (expandedFolderId == null) return
        confirmDeleteVisible = false
        actionMenuVisible = !actionMenuVisible
        populateExpandedHeader()
    }

    private fun onFolderAction(word: Int) {
        val folderId = expandedFolderId ?: return
        if (confirmDeleteVisible) {
            when (word) {
                1 -> { // yes, delete
                    folderStore.delete(folderId)
                    exitIntentionView()
                }
                2 -> { // no, cancel
                    confirmDeleteVisible = false
                    populateExpandedHeader()
                }
            }
            return
        }
        when (word) {
            1 -> showAppList(Constants.FLAG_ADD_TO_FOLDER, folderId = folderId)
            2 -> renameIntention(folderId)
            3 -> {
                confirmDeleteVisible = true
                populateExpandedHeader()
            }
        }
    }

    private fun launchFolderApp(packageName: String) {
        try {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            if (intent == null) {
                requireContext().showToast(getString(R.string.folder_app_missing))
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            requireContext().showToast(getString(R.string.folder_app_missing))
        }
    }

    private sealed interface HomeRow {
        data class FolderRow(val folder: VerbFolder) : HomeRow
        data class AppRow(val folderId: Long, val member: Member) : HomeRow
        data object EmptyRow : HomeRow
    }

    companion object {
        private const val FOLDER_TAG = "folder:"
        private const val FAPP_TAG = "fapp:"
        private const val EMPTY_TAG = "empty:"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}