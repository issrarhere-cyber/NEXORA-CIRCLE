package com.nexora.circle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.LaunchedEffect
import com.nexora.circle.ui.drawer.AnonymousMode
import com.nexora.circle.ui.drawer.CustomizeThemeScreen
import com.nexora.circle.ui.drawer.DrawerRoute
import com.nexora.circle.ui.drawer.HelpCentreScreen
import com.nexora.circle.ui.drawer.LearnSkillsScreen
import com.nexora.circle.ui.drawer.MyLibraryScreen
import com.nexora.circle.ui.drawer.NexoraDrawer
import com.nexora.circle.ui.drawer.NexusScreen
import com.nexora.circle.ui.drawer.SettingsScreen
import com.nexora.circle.ui.components.NexoraIcon
import com.nexora.circle.ui.components.NxIcon
import androidx.compose.ui.graphics.vector.ImageVector
import com.nexora.circle.ui.components.StarfieldBackground
import com.nexora.circle.ui.screens.NexoraXScreen
import com.nexora.circle.ui.screens.PlaceholderScreen
import com.nexora.circle.ui.theme.NexoraColors

private enum class Tab(val label: String, val icon: ImageVector) {
    Orbit("Orbit", Icons.Filled.Home),
    Comms("Comms", Icons.Filled.Email),
    Arcade("Arcade", Icons.Filled.PlayArrow),
    NexoraX("Nexora-X", Icons.Filled.Star),
    Profile("Me", Icons.Filled.Person),
}

@Composable
fun NexoraApp() {
    var selected by rememberSaveable { mutableStateOf(Tab.Orbit) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showSignal by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var profileUid by rememberSaveable { mutableStateOf("") }
    var beamUid by rememberSaveable { mutableStateOf("") }
    // Me tab sub-screens: "" = none, or "posts" / "followers" / "following" / "x:<label>"
    var meSub by rememberSaveable { mutableStateOf("") }
    var meUserUid by rememberSaveable { mutableStateOf("") }
    // Left drawer: open/closed, and which drawer screen is open ("" = none)
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var drawerRoute by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    // Anonymous Mode: load my saved switch and listen for other people's switches
    val signedInUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    LaunchedEffect(signedInUid) {
        AnonymousMode.start(context, signedInUid)
    }

    // While typing in Comms: hide the bottom bar and drop its padding, because the
    // chat screen already adds keyboard padding itself (otherwise the gap is counted twice).
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val hideBar = imeVisible && selected == Tab.Comms
    val layoutDirection = LocalLayoutDirection.current

    // System back button: close the open sub-screen first, then return to the Orbit tab,
    // and only then let the app close. (Placed before the content so a screen that has
    // its own BackHandler, e.g. a chat, still takes priority.)
    val subScreenOpen = profileUid.isNotEmpty() || showSearch || showSignal || showNotifications ||
        meSub.isNotEmpty() || meUserUid.isNotEmpty()
    BackHandler(enabled = subScreenOpen || selected != Tab.Orbit) {
        when {
            meUserUid.isNotEmpty() -> meUserUid = ""
            meSub.isNotEmpty() -> meSub = ""
            profileUid.isNotEmpty() -> profileUid = ""
            showSearch -> showSearch = false
            showSignal -> showSignal = false
            showNotifications -> showNotifications = false
            else -> selected = Tab.Orbit
        }
    }

    StarfieldBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (!hideBar) {
                    NavigationBar(containerColor = NexoraColors.Panel) {
                        Tab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = selected == tab,
                                onClick = {
                                    selected = tab
                                    showSearch = false
                                    showSignal = false
                                    showNotifications = false
                                    profileUid = ""
                                    beamUid = ""
                                    meSub = ""
                                    meUserUid = ""
                                    drawerRoute = ""
                                },
                                icon = {
                                    TabIcon(
                                        tab,
                                        if (selected == tab) NexoraColors.SpaceBlack else NexoraColors.Muted,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NexoraColors.SpaceBlack,
                                    selectedTextColor = NexoraColors.PlasmaCyan,
                                    indicatorColor = NexoraColors.PlasmaCyan,
                                    unselectedIconColor = NexoraColors.Muted,
                                    unselectedTextColor = NexoraColors.Muted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding: PaddingValues ->
            val contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection),
                bottom = if (hideBar) 0.dp else padding.calculateBottomPadding(),
            )
            Box(Modifier.fillMaxSize().padding(contentPadding)) {
                if (drawerRoute.isNotEmpty()) {
                    val closeDrawerScreen = { drawerRoute = "" }
                    when (drawerRoute) {
                        DrawerRoute.NEXUS -> NexusScreen(onBack = closeDrawerScreen)
                        DrawerRoute.LIBRARY -> MyLibraryScreen(onBack = closeDrawerScreen)
                        DrawerRoute.SKILLS -> LearnSkillsScreen(onBack = closeDrawerScreen)
                        DrawerRoute.THEME -> CustomizeThemeScreen(onBack = closeDrawerScreen)
                        DrawerRoute.SETTINGS -> SettingsScreen(onBack = closeDrawerScreen)
                        DrawerRoute.HELP -> HelpCentreScreen(onBack = closeDrawerScreen)
                        DrawerRoute.FOLLOWING -> UserListScreen(
                            title = "Following",
                            ownerUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                            collection = "following",
                            onBack = closeDrawerScreen,
                            onUserClick = { uid ->
                                drawerRoute = ""
                                selected = Tab.Orbit
                                profileUid = uid
                            },
                        )
                        DrawerRoute.FOLLOWERS -> UserListScreen(
                            title = "Followers",
                            ownerUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                            collection = "followers",
                            onBack = closeDrawerScreen,
                            onUserClick = { uid ->
                                drawerRoute = ""
                                selected = Tab.Orbit
                                profileUid = uid
                            },
                        )
                        else -> drawerRoute = ""
                    }
                } else when (selected) {
                    Tab.Orbit -> {
                        if (profileUid.isNotEmpty()) {
                            UserProfileScreen(
                                uid = profileUid,
                                onBack = { profileUid = "" },
                                onBeam = { uid ->
                                    beamUid = uid
                                    profileUid = ""
                                    selected = Tab.Comms
                                },
                            )
                        } else if (showSearch) {
                            SearchScreen(
                                onBack = { showSearch = false },
                                onUserClick = { uid -> profileUid = uid },
                            )
                        } else if (showSignal) {
                            SignalScreen(onBack = { showSignal = false })
                        } else if (showNotifications) {
                            NotificationsScreen(onBack = { showNotifications = false })
                        } else {
                            OrbitScreen(
                                onOpenComms = { selected = Tab.Comms },
                                onOpenNexoraX = { selected = Tab.NexoraX },
                                onOpenProfile = { selected = Tab.Profile },
                                onOpenSearch = { showSearch = true },
                                onOpenSignal = { showSignal = true },
                                onOpenNotifications = { showNotifications = true },
                                onOpenUserProfile = { uid -> profileUid = uid },
                                onOpenDrawer = { drawerOpen = true },
                            )
                        }
                    }
                    Tab.Comms -> CommsScreen(startUid = beamUid.ifEmpty { null })
                    Tab.Arcade -> PlaceholderScreen(
                        title = "Arcade (Games)",
                        stage = "Stage 6",
                        planned = listOf(
                            "Space puzzles, alien mysteries, brain and memory challenges",
                            "Nexora-X decoding games",
                            "Multiplayer quiz battles",
                            "Friends leaderboard and daily challenges",
                        ),
                    )
                    Tab.NexoraX -> NexoraXScreen()
                    Tab.Profile -> PlaceholderScreen(
                        title = "Profile",
                        stage = "Stage 2",
                        planned = listOf(
                            "Registration and secure login",
                            "Profile and profile picture",
                            "Follow and friends",
                            "Privacy settings",
                        ),
                    )
                }
            }
        }

        // Left drawer (opened from the profile icon at the top left of Orbit)
        NexoraDrawer(
            open = drawerOpen,
            onClose = { drawerOpen = false },
            onProfile = {
                drawerOpen = false
                drawerRoute = ""
                meSub = ""
                meUserUid = ""
                selected = Tab.Profile
            },
            onNavigate = { route ->
                drawerOpen = false
                drawerRoute = route
            },
        )
    }
}
