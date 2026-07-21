package com.animk.app.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animk.app.data.repository.AuthRepository
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSheet(
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = custom.surface,
        contentColor = custom.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRegisterMode) "Register AnimK Account" else "Login to AnimK",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = custom.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isRegisterMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = custom.primary,
                        unfocusedBorderColor = custom.textSecondary.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = custom.primary,
                    unfocusedBorderColor = custom.textSecondary.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = custom.primary,
                    unfocusedBorderColor = custom.textSecondary.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (isRegisterMode && username.isBlank())) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val success = if (isRegisterMode) {
                            authRepository.signUp(email.trim(), password.trim(), username.trim())
                        } else {
                            authRepository.signIn(email.trim(), password.trim())
                        }
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, if (isRegisterMode) "Registration successful!" else "Logged in successfully!", Toast.LENGTH_SHORT).show()
                            onAuthSuccess()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Authentication failed. Check your credentials.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = custom.primary,
                    contentColor = custom.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = custom.onPrimary)
                } else {
                    Text(
                        text = if (isRegisterMode) "Sign Up" else "Log In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { isRegisterMode = !isRegisterMode }
            ) {
                Text(
                    text = if (isRegisterMode) "Already have an account? Log In" else "Don't have an account? Register",
                    color = custom.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
