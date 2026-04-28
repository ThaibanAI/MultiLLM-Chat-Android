package com.thaibanai.multillmchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thaibanai.multillmchat.ui.screens.chat.ChatScreen
import com.thaibanai.multillmchat.ui.screens.conversations.ConversationsScreen
import com.thaibanai.multillmchat.ui.screens.settings.SettingsScreen
import com.thaibanai.multillmchat.ui.theme.MultiLLMTheme
import com.thaibanai.multillmchat.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String) {
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    data object Conversations : Screen("conversations")
    data object Settings : Screen("settings")
    data object NewChat : Screen("chat/new")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme = themeViewModel.isDarkTheme

            MultiLLMTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Conversations.route
                    ) {
                        composable(Screen.Conversations.route) {
                            ConversationsScreen(
                                onChatClick = { conversationId ->
                                    navController.navigate(Screen.Chat.createRoute(conversationId))
                                },
                                onNewChat = {
                                    navController.navigate(Screen.NewChat.route)
                                },
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }
                        composable(Screen.Chat.route) {
                            ChatScreen(
                                onBackClick = { navController.popBackStack() },
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onNavigateToConversations = {
                                    navController.navigate(Screen.Conversations.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.NewChat.route) {
                            ChatScreen(
                                onBackClick = { navController.popBackStack() },
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onNavigateToConversations = {
                                    navController.navigate(Screen.Conversations.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
