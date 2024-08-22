package com.galactapp.autenticatorcompose

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galactapp.autenticatorcompose.presentation.profile.ProfileScreen
import com.galactapp.autenticatorcompose.presentation.sign_in.GoogleAuthUiClient
import com.galactapp.autenticatorcompose.presentation.sign_in.SignInScreen
import com.galactapp.autenticatorcompose.presentation.sign_in.SignInViewModel
import com.galactapp.autenticatorcompose.ui.theme.AutenticatorComposeTheme
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Inicializa um cliente personalizado para lidar com a autenticação do Google.
    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext) // Inicializa o One Tap Client.
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Habilita o modo "edge-to-edge" para a UI.

        setContent {
            AutenticatorComposeTheme {
                // Configura a Surface que contém toda a UI, usando a cor de fundo do tema.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    //HomeScreen
                    //HomeScreenNew

                    val navController = rememberNavController() // Inicializa o controlador de navegação.

                    // Configura o NavHost, que gerencia as diferentes telas (composables) e navegação.
                    NavHost(navController = navController, startDestination = "sign_in") {
                        // Define a tela de sign in (login).
                        composable("sign_in") {
                            val viewModel = viewModel<SignInViewModel>() // Obtém o ViewModel responsável pela tela de sign in.
                            val state by viewModel.state.collectAsStateWithLifecycle() // Observa o estado da tela de sign in, respeitando o ciclo de vida.

                            // Verifica se o usuário já está logado. Se sim, navega para a tela de perfil.
                            LaunchedEffect(key1 = Unit) {
                                if (googleAuthUiClient.getSignedInUser() != null) {
                                    navController.navigate("profile")
                                }
                            }

                            // Inicializa um launcher para o resultado da Activity de login.
                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        lifecycleScope.launch {
                                            // Processa o resultado do login usando o cliente de autenticação.
                                            val signInResult = googleAuthUiClient.signInWithIntent(
                                                intent = result.data ?: return@launch
                                            )
                                            viewModel.onSignInResult(signInResult) // Atualiza o estado da tela de sign in com o resultado.
                                        }
                                    }
                                }
                            )

                            // Observa se o login foi bem-sucedido e, se sim, navega para a tela de perfil.
                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign in successful",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    navController.navigate("profile") // Navega para a tela de perfil.
                                    viewModel.resetState() // Reseta o estado do ViewModel após o login.
                                }
                            }

                            // Exibe a tela de sign in, passando o estado e a ação para iniciar o login.
                            SignInScreen(
                                state = state,
                                onSignInClick = {
                                    lifecycleScope.launch {
                                        // Solicita o intent para iniciar o processo de sign in com o Google.
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        launcher.launch(
                                            IntentSenderRequest.Builder(
                                                signInIntentSender ?: return@launch
                                            ).build()
                                        )
                                    }
                                }
                            )
                        }

                        // Define a tela de perfil do usuário.
                        composable("profile") {
                            ProfileScreen(
                                userData = googleAuthUiClient.getSignedInUser(), // Obtém os dados do usuário logado.
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut() // Faz o logout do usuário.
                                        Toast.makeText(
                                            applicationContext,
                                            "Signed out",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        navController.popBackStack() // Volta para a tela anterior (sign in).
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

