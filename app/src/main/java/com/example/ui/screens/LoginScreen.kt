package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ElbeitBrandLogo
import com.example.ui.components.LuxLogoBadge
import com.example.ui.components.LuxPanel
import com.example.ui.components.MosqueSkyline
import com.example.ui.components.OrnamentDivider
import com.example.ui.components.arabesqueLattice
import com.example.ui.theme.CrimsonErrorContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldDark
import com.example.ui.theme.RoyalGoldLight
import com.example.util.LocalAppStrings

@Composable
fun LoginScreen(
    onLoginAttempt: (String, String, () -> Unit) -> Unit,
    onToggleLanguage: () -> Unit
) {
    val strings = LocalAppStrings.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var hasError by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(EmeraldDark, ObsidianNight, MidnightSurface)
                )
            )
            .arabesqueLattice(alpha = 0.09f)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        MosqueSkyline(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(84.dp)
        )

        // Top bar with 🌐 Language Toggle Button (Section 3.1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.versionTag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("language_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = strings.languageLabel,
                    tint = RoyalGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (strings.isArabic) "🌐 English" else "🌐 العربية",
                    color = RoyalGoldLight,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            LuxLogoBadge(badgeSize = 86.dp)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "البيت العربي",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                ),
                color = RoyalGoldLight
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.companySubtitle.ifBlank { "مناسباتك.. تنظيم أسهل" },
                style = MaterialTheme.typography.bodyMedium,
                color = IvoryText.copy(alpha = 0.9f),
                modifier = Modifier
                    .background(RoyalGold.copy(alpha = 0.15f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            LuxPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.loginTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = RoyalGoldLight
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    AnimatedVisibility(visible = hasError) {
                        Surface(
                            color = CrimsonErrorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .testTag("login_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.loginError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            hasError = false
                        },
                        label = { Text(strings.usernameLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = strings.usernameLabel, tint = RoyalGold)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalGold,
                            unfocusedBorderColor = RoyalGold.copy(alpha = 0.35f),
                            focusedLabelColor = RoyalGoldLight,
                            cursorColor = RoyalGold,
                            focusedContainerColor = Color.Black.copy(alpha = 0.22f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.22f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            hasError = false
                        },
                        label = { Text(strings.passwordLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = strings.passwordLabel, tint = RoyalGold)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = strings.passwordLabel
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalGold,
                            unfocusedBorderColor = RoyalGold.copy(alpha = 0.35f),
                            focusedLabelColor = RoyalGoldLight,
                            cursorColor = RoyalGold,
                            focusedContainerColor = Color.Black.copy(alpha = 0.22f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.22f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onLoginAttempt(username, password) { hasError = true }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = {
                            onLoginAttempt(username, password) { hasError = true }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = ObsidianNight
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = RoyalGold,
                                spotColor = RoyalGold
                            )
                            .background(
                                Brush.linearGradient(listOf(RoyalGoldLight, RoyalGold, RoyalGoldDark)),
                                RoundedCornerShape(14.dp)
                            )
                            .testTag("login_button")
                    ) {
                        Text(
                            text = strings.signInButton,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OrnamentDivider(modifier = Modifier.padding(horizontal = 40.dp))
        }
    }
}
