package com.galactapp.autenticatorcompose

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun Homescreen(){

    // `user` armazena o usuário atual autenticado. Se o usuário não estiver autenticado, `user` será nulo.
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }

    // `launcher` é o gerenciador de resultado para a autenticação Google.
    var laucher = authLuncher(
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
    val tokrn = stringResource(id = R.string.web_id)
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
        if(user == null){

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
                    // Configura as opções de login do Google, incluindo a solicitação do token de ID e e-mail.
                    val gso =
                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(tokrn)
                            .requestEmail()
                            .build()

                    // Obtém o cliente de login do Google com as opções configuradas.
                    val gsc = GoogleSignIn.getClient(context, gso)

                    // Inicia a atividade de login com o Google.
                    laucher.launch(gsc.signInIntent)
                },
                colors = ButtonDefaults.buttonColors(Color.White)
            ){

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
            ){
                Text(
                    text = "Sign out",
                    color = Color.Black
                )
            }
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun authLuncher(
    // Callback para quando a autenticação é completada com sucesso.
    onAuthComplete: (AuthResult) -> Unit,
    // Callback para quando ocorre um erro na autenticação.
    onAuthError: (ApiException) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {

    // Cria um escopo de coroutine para gerenciar operações assíncronas.
    val scope = rememberCoroutineScope()

    // Cria e retorna um gerenciador de resultado para a atividade de autenticação.
    return rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        // Obtém o resultado do login com o Google a partir da intent retornada.
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // Obtém a conta do Google a partir da intent.
            val account = task.getResult(ApiException::class.java)!!
            // Cria as credenciais do Firebase a partir do token de ID da conta do Google.
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)

            // Inicia uma coroutine para realizar o login no Firebase.
            scope.launch {
                // Tenta realizar o login com as credenciais obtidas.
                val authResult = Firebase.auth.signInWithCredential(credential).await()
                // Chama o callback de sucesso.
                onAuthComplete(authResult)
            }
        } catch(e: ApiException) {
            // Se ocorrer um erro, chama o callback de erro.
            onAuthError(e)
        }
    }
}