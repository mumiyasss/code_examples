package com.md.matur.feature.base

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.md.matur.BuildConfig
import com.md.matur.NailsApplication
import com.md.matur.R
import com.md.matur.data.entity.entry.CalendarEntry
import com.md.matur.data.utils.ChatEntity
import com.md.matur.debug
import com.md.matur.feature.auth.advicetologin.AdviceLoginFragment
import com.md.matur.feature.auth.advicetologin.AdviceLoginFragment.Companion.FragmentListEnum.*
import com.md.matur.feature.offers.available.AvailableFragment
import com.md.matur.feature.entry.detail.client.EntryDetailClientFragment
import com.md.matur.feature.entry.detail.master.EntryDetailMasterFragment
import com.md.matur.feature.entry.list.EntryListFragment
import com.md.matur.feature.entry.signup.submit.SubmitFragment
import com.md.matur.feature.offers.map.MapFragment
import com.md.matur.feature.messenger.chatscreen.ChatScreenFragment
import com.md.matur.feature.messenger.dialogs.DialogsFragment
import com.md.matur.feature.profile.client.ProfileClientFragment
import com.md.matur.feature.profile.client.favorite.FavoritesFragment
import com.md.matur.feature.profile.master.main.ProfileMasterFragment
import com.md.matur.feature.offers.public_offer.MasterPublicOfferFragment
import com.md.matur.data.repository.NailsApiService
import com.md.matur.feature.base.mvvm.BaseFragment
import com.md.matur.feature.base.splash.UpdateActivity
import com.md.matur.feature.entry.calendar.EntryCalendar
import com.md.matur.feature.entry.detail.client.map.OfferOnMapFragment
import com.md.matur.feature.profile.master.finance.FinanceFragment
import com.md.matur.feature.statistics.StatisticsFragment
import com.md.matur.feature.statistics.StatisticsViewModel
import com.md.matur.utils.*
import com.md.matur.utils.extensions.getPermissionString
import com.md.matur.utils.extensions.hasPermission
import com.md.matur.utils.extensions.showToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import java.util.*
import javax.inject.Inject
import kotlin.NoSuchElementException
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity(), PermissionHandler {

    /**
     * Здесь сохраняется CallBack, который будет
     * вызван после предоставленя прав доступа.
     */
    private var actionOnPermission: ((granted: Boolean) -> Unit) = {}

    companion object {
        const val onClickKey: String = "alksdjfal;skjf"
        var logged = false
        const val IS_MASTER_KEY = "asdfjalskjdfhrufaskdfnafinf"
        const val PERMISSION_REQUEST_CODE = 100
        var pendingFragmentForClient: Fragment? = null
        var isMaster: Boolean = false
    }

    @Inject
    lateinit var config: Config

    val masterVm by viewModels<MasterMainViewModel>()
    val mainVm by viewModels<MainViewModel>()

    private val fragmentContainerId = R.id.container

    // Client specific fragments
    private val mapFragment by lazy { MapFragment() }
    private val availableFragment by lazy { AvailableFragment() }
    private val dialogsFragment by lazy { DialogsFragment() }
    private val profileClientFragment by lazy { ProfileClientFragment() }
    private val clientEntryListFragment by lazy { EntryListFragment.newInstance(CLIENT_ROLE) }

    // Master specific fragments
    private val profileMasterFragment by lazy { ProfileMasterFragment() }
    private val calendarMasterFragment by lazy { EntryCalendar() }

    val statisticsFragment by lazy { StatisticsFragment() }
    private var currentFragment: Fragment? = null


    private lateinit var activeNavigationListener: BottomNavigationView.OnNavigationItemSelectedListener

    /**
     * Local back stack
     */
    private val localBackStack = LinkedList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        NailsApplication.appComponent.inject(this)
        isMaster = config.loggedAsMaster && config.token.isNotEmpty()

        intent.getStringExtra(onClickKey)?.let {
            val splittedCommand = it.split("/")
            when (splittedCommand.firstOrNull()) {
                "entry_client" -> {
                    config.loggedAsMaster = false
                    isMaster = false
                }
                "entry_master" -> {
                    config.loggedAsMaster = true
                    isMaster = true
                }
            }
        }


        if (isMaster) {
            pendingFragmentForClient = null
            // For master
            activeNavigationListener = masterOnNavigationItemSelectedListener
            navigation.menu.clear()
            navigation.inflateMenu(R.menu.master_navigation)
            navigation.setOnNavigationItemSelectedListener(activeNavigationListener)
            config.currentProfileLiveData.observe(this, {
                it?.offer?.let { offer ->
                    masterVm.updateOffer(offer)
                }
            })
            navigation.selectedItemId = R.id.nav_profile
        } else {
            // For client
            navigation.menu.clear()
            navigation.inflateMenu(R.menu.client_navigation)
            activeNavigationListener = clientOnNavigationItemSelectedListener
            navigation.setOnNavigationItemSelectedListener(activeNavigationListener)
            navigation.selectedItemId = R.id.nav_map
            logged = getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE)
                .getBoolean(Constants.LOGGED, false)
        }


        intent.getStringExtra(onClickKey)?.let {
            navigateFromPush(it)
        }

    }

    private fun navigateFromPush(onClick: String) {
        val splittedCommand = onClick.split("/")

        // todo: переключить подсветку в bottom баре
        when (splittedCommand.firstOrNull()) {
            "chat" -> {
                asyncOnIOThread {
                    try {
                        val chatEntity: ChatEntity =
                            mainVm.getChatEntityById(splittedCommand.last())
                        asyncOnMainThread {
                            loadFragment(
                                ChatScreenFragment.newInstance(
                                    chatId = chatEntity.id,
                                    companion = chatEntity.companion
                                )
                            )
                        }
                    } catch (e: Exception) {
                        showToast(handleNetExceptions(e))
                    }
                }
            }
            "entry_client" -> {
                loadFragment(
                    EntryDetailClientFragment.newInstance(entryId = splittedCommand.last())
                )
            }
            "entry_master" -> {
                loadFragment(
                    EntryDetailMasterFragment.newInstance(entryId = splittedCommand.last())
                )
            }
        }
    }

    private val masterOnNavigationItemSelectedListener by lazy {
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_available -> loadFragment(profileMasterFragment)
                R.id.nav_chats -> loadFragment(dialogsFragment)
                R.id.nav_profile -> loadFragment(profileMasterFragment)
                R.id.nav_statistics -> loadFragment(statisticsFragment)
                R.id.nav_orders -> loadFragment(calendarMasterFragment)
            }
            true
        }
    }

    private val clientOnNavigationItemSelectedListener by lazy {
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> loadFragment(mapFragment)
                else -> {
                    if (logged)
                        when (item.itemId) {
                            R.id.nav_available -> loadFragment(availableFragment)
                            R.id.nav_chats -> loadFragment(dialogsFragment)
                            R.id.nav_profile -> loadFragment(profileClientFragment)
                            R.id.nav_orders -> loadFragment(clientEntryListFragment)
                        }
                    else
                        when (item.itemId) {
                            R.id.nav_available -> loadFragment(AdviceLoginFragment().apply {
                                expectedFragment = Available
                            })
                            R.id.nav_chats -> loadFragment(AdviceLoginFragment().apply {
                                expectedFragment = Dialogs
                            })
                            R.id.nav_profile -> loadFragment(AdviceLoginFragment().apply {
                                expectedFragment = Profile
                            })
                            R.id.nav_orders -> loadFragment(AdviceLoginFragment().apply {
                                expectedFragment = Entries
                            })
                        }
                }
            }
            true
        }
    }

    val animation = object : Animation() {
        override fun applyTransformation(
            interpolatedTime: Float, t: Transformation?
        ) {
            val params = navigation.layoutParams as LinearLayout.LayoutParams
            params.bottomMargin =
                (-navigation.height * interpolatedTime).toInt()
            navigation.layoutParams = params
        }
    }.apply { duration = 600 }

    private fun showHideBottomBar(fragment: Fragment) {

        when (fragment) {
            is MapFragment,
            is ProfileClientFragment,
            is AvailableFragment,
            is MasterPublicOfferFragment,
            is EntryListFragment,
            is FavoritesFragment,
            is EntryDetailMasterFragment,
            is EntryDetailClientFragment,
            is ProfileMasterFragment,
            is DialogsFragment,
            is OfferOnMapFragment,
            is EntryCalendar,
            is StatisticsFragment,
            is AdviceLoginFragment -> {
                if (isNavigationBarCollapsed) {
                    animation.cancel()
                    animation.reset()
                    (navigation.layoutParams as LinearLayout.LayoutParams).bottomMargin = 0
                    isNavigationBarCollapsed = false
                }
            }
            else -> {
                if (isNavigationBarCollapsed.not()) {
                    container.startAnimation(animation)
                    isNavigationBarCollapsed = true
                }
            }
        }
    }

    var upgradeToMasterDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        asyncOnBackgroundThread {
            delay(100)
            updateProfile()
            upgradeToMasterDisposable = upgradedToMasterFromSocketBus
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    config.loggedAsMaster = true
                    startActivity(Intent(this@MainActivity, MainActivity::class.java).apply {
                        putExtra(IS_MASTER_KEY, true)
                    })
                }
            asyncOnMainThread {
                lifecycle.addObserver(mainVm)
            }
        }
    }

    override fun onPause() {
        upgradeToMasterDisposable?.dispose()
        super.onPause()
    }

    private var isNavigationBarCollapsed = true

    @Synchronized
    fun loadFragment(
        fragment: Fragment,
        addToBackStack: Boolean = true,
        howMuchRemove: Int = 0
    ) {
        synchronized(localBackStack) {
            if (fragment == currentFragment)
                return
            else currentFragment = fragment
            supportFragmentManager.beginTransaction().run {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                supportFragmentManager.fragments.forEach {
                    if (it != fragment && it.isAdded) {
                        hide(it)
                    }
                }

                if (fragment.isAdded)
                    show(fragment)
                else
                    add(fragmentContainerId, fragment)
                showHideBottomBar(fragment)
                commit()
            }

            // Помещаем каждый фрагмент в локальный BackStack,
            // игнорируем то что идет из onBackPressed()
            if (addToBackStack)
                localBackStack.add(fragment)
            if (!addToBackStack || howMuchRemove > 0) {
                selectBottomFragmentIndicator(fragment)
                delayed(100) {
                    showHideBottomBar(fragment)
                }
            }

        }
    }

    /**
     * На время убирает слушателя BottomNavigation,
     * чтобы он зря не вызывал loadFragment(),
     * а просто бы поменялся активный индикатор.
     */
    private fun selectBottomFragmentIndicator(fragment: Fragment) {
        navigation.setOnNavigationItemSelectedListener(null)
        navigation.selectedItemId =
            when (fragment) {
                is MapFragment -> R.id.nav_map
                is ProfileClientFragment -> R.id.nav_profile
                is AvailableFragment -> R.id.nav_available
                is AdviceLoginFragment -> R.id.nav_profile
                is DialogsFragment -> R.id.nav_chats
                is ProfileMasterFragment -> R.id.nav_profile
                is EntryCalendar -> R.id.nav_orders
                is EntryListFragment -> R.id.nav_orders
                else -> {
                    navigation.selectedItemId
                }
            }
        navigation.setOnNavigationItemSelectedListener(activeNavigationListener)
    }

    /**
     * Происходит работа с кастомным back stack.
     */
    @Synchronized
    override fun onBackPressed() {
        synchronized(localBackStack) {
            if (localBackStack.lastOrNull() is BaseFragment) {
                if ((localBackStack.last as BaseFragment).onBackPressed()) return
            }
            try {
                loadFragment(
                    localBackStack[localBackStack.size - 2],
                    addToBackStack = false,
                    howMuchRemove = 1
                )
            } catch (e: IndexOutOfBoundsException) {
                super.onBackPressed()
            }
        }
    }

    /**
     * Позволяет сделать несколько шагов назад мгновенно
     */
    @Synchronized
    fun deepBack(howMuchRemove: Int = 1) {
        synchronized(localBackStack) {
            if (localBackStack.lastOrNull() is BaseFragment) {
                if ((localBackStack.last as BaseFragment).onBackPressed()) return
            }
            try {
                loadFragment(
                    localBackStack[localBackStack.size - 1 - howMuchRemove],
                    addToBackStack = false,
                    howMuchRemove = howMuchRemove
                )
            } catch (e: IndexOutOfBoundsException) {
                super.onBackPressed()
            }
        }
    }

    /**
     * Для работы с Permissions.
     * @param permissionId - Android Permission Id.
     * @param callback - что нужно сделать, после предоставления прав доступа.
     */
    override fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        if (hasPermission(permissionId)) {
            callback(true)
        } else {
            actionOnPermission = callback
            ActivityCompat.requestPermissions(
                this,
                arrayOf(getPermissionString(permissionId)),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            actionOnPermission.invoke(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    fun hideNavigationBar() {
        if (isNavigationBarCollapsed.not()) {
            navigation.gone()
            isNavigationBarCollapsed = true
        }
    }

    fun showNavigationBar() {
        if (isNavigationBarCollapsed) {
            navigation.visible()
            isNavigationBarCollapsed = false
        }
    }

    fun updateProfile() {
        mainVm.updateProfileInfo()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val focusedView = currentFocus
            if (focusedView is EditText && (focusedView is AppCompatEditText).not()) {
                val outRect = Rect()
                focusedView.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0)
                    focusedView.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
