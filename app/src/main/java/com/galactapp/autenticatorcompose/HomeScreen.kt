package com.galactapp.autenticatorcompose

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun Homesscreen() {

    // `user` armazena o usuário atual autenticado. Se o usuário não estiver autenticado, `user` será nulo.
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }

    // `launcher` é o gerenciador de resultado para a autenticação Google.
    val launcher = authLauncher(
        onAuthComplete = { result ->
            // Quando a autenticação é concluída com sucesso, o usuário é atualizado.
            user = result.user
        },
        onAuthError = {
            // Se ocorrer um erro na autenticação, `user` é definido como nulo.
            user = null
        }
    )

    // Obtém o ID do cliente web do arquivo de recursos.
    val token = stringResource(id = R.string.web_id)
    // Obtém o contexto atual da composição.
    val context = LocalContext.current

    // Define uma coluna com alinhamento central e preenche todo o espaço disponível.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {

        // Verifica se o usuário não está autenticado.
        if (user == null) {

            // Exibe o texto informando que o usuário não está logado.
            Text(
                text = "Not logged in",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Botão para iniciar o processo de login com o Google.
            Button(
                onClick = {
                    // Configura as opções de login do Google usando o novo Google Identity Services (GIS).
                    val signInRequest = BeginSignInRequest.builder()
                        .setGoogleIdTokenRequestOptions(
                            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(token) // Use o client ID correto
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                        )
                        .build()

                    val oneTapClient = Identity.getSignInClient(context)

                    oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener { result ->
                            try {
                                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                launcher.launch(intentSenderRequest)
                            } catch (e: IntentSender.SendIntentException) {
                                Log.e("Homescreen", "Couldn't start One Tap UI: ${e.localizedMessage}")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Homescreen", "Sign-in failed: ${e.localizedMessage}")
                        }
                },
                colors = ButtonDefaults.buttonColors(Color.White)
            ) {

                // Define o layout do botão com um ícone do Google e o texto "Sign in with Google".
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        color = Color.Black
                    )
                }
            }
        } else {
            // Se o usuário estiver autenticado, exibe a foto de perfil e o nome do usuário.

            AsyncImage(
                model = "${user!!.photoUrl}",
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Text(
                text = "${user!!.displayName}",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Botão para fazer logout.
            Button(
                onClick = {
                    Firebase.auth.signOut()
                    user = null
                },
                colors = ButtonDefaults.buttonColors(Color.White)
            ) {
                Text(
                    text = "Sign out",
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun authLauncher(
    // Callback para quando a autenticação é completada com sucesso.
    onAuthComplete: (AuthResult) -> Unit,
    // Callback para quando ocorre um erro na autenticação.
    onAuthError: (Exception) -> Unit
): ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult> {

    // Cria um escopo de coroutine para gerenciar operações assíncronas.
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // Cria e retorna um gerenciador de resultado para a atividade de autenticação.
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = Identity.getSignInClient(context)
                    .getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    scope.launch {
                        val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
                        onAuthComplete(authResult)
                    }
                }
            } catch (e: ApiException) {
                onAuthError(e)
            }
        } else {
            onAuthError(Exception("Sign-in failed or canceled"))
        }
    }
}
