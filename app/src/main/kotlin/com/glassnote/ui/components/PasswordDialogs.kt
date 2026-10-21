package com.glassnote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glassnote.R
import com.glassnote.ui.theme.isDarkGlassTheme
import kotlinx.coroutines.delay

enum class PasswordDialogMode {
    ENCRYPT,
    UNLOCK,
    REMOVE_ENCRYPTION,
}

@Composable
fun PasswordDialog(
    mode: PasswordDialogMode,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val darkTheme = isDarkGlassTheme()
    val shape = RoundedCornerShape(28.dp)

    /*
     * MINIMAL and FULL deliberately share the same opaque sheet. A Compose
     * Dialog is a separate window and cannot sample/blur the host window, so a
     * translucent FULL card would always let the home text bleed through.
     * The card is alpha 1.0; only the outside scrim is dimmed.
     */
    val scrimColor = Color.Black.copy(alpha = 0.42f)
    val cardColor = if (darkTheme) Color(0xFF1E1E1E) else Color.White
    val borderColor = if (darkTheme) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldFill = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant

    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val passwordFocus = remember { FocusRequester() }

    val mismatch = stringResource(R.string.password_error_mismatch)
    val empty = stringResource(R.string.password_error_empty)

    val title = stringResource(
        when (mode) {
            PasswordDialogMode.ENCRYPT -> R.string.password_encrypt_title
            PasswordDialogMode.UNLOCK -> R.string.password_unlock_title
            PasswordDialogMode.REMOVE_ENCRYPTION -> R.string.password_remove_title
        },
    )
    val body = stringResource(
        when (mode) {
            PasswordDialogMode.ENCRYPT -> R.string.password_encrypt_body
            PasswordDialogMode.UNLOCK -> R.string.password_unlock_body
            PasswordDialogMode.REMOVE_ENCRYPTION -> R.string.password_remove_body
        },
    )
    val confirmLabel = stringResource(
        when (mode) {
            PasswordDialogMode.ENCRYPT -> R.string.password_confirm_encrypt
            PasswordDialogMode.UNLOCK -> R.string.password_unlock_action
            PasswordDialogMode.REMOVE_ENCRYPTION -> R.string.password_remove_action
        },
    )
    val passwordPlaceholder = stringResource(R.string.password_placeholder)

    LaunchedEffect(Unit) {
        delay(120)
        passwordFocus.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Opaque/semi-opaque base below GlassSurface: GlassSurface's own
            // gradient remains, but the home page can no longer read through it.
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .shadow(elevation = 28.dp, shape = shape)
                    .clip(shape)
                    .background(cardColor)
                    .border(width = 1.dp, color = borderColor, shape = shape),
            ) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    contentPadding = PaddingValues(24.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = onSurface,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryText.copy(alpha = 0.9f),
                        )

                        if (mode == PasswordDialogMode.ENCRYPT) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.password_warning),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                localError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(fieldFill, fieldShape)
                                .focusRequester(passwordFocus),
                            label = {
                                Text(
                                    text = stringResource(R.string.password_label),
                                    color = secondaryText.copy(alpha = 0.82f),
                                )
                            },
                            placeholder = {
                                Text(
                                    text = passwordPlaceholder,
                                    color = secondaryText.copy(alpha = 0.72f),
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = onSurface,
                            ),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                            ),
                            shape = fieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = onSurface,
                                unfocusedTextColor = onSurface,
                                focusedBorderColor = primary,
                                unfocusedBorderColor = secondaryText.copy(alpha = 0.58f),
                                cursorColor = primary,
                                focusedLabelColor = primary,
                                unfocusedLabelColor = secondaryText.copy(alpha = 0.82f),
                                focusedPlaceholderColor = secondaryText.copy(alpha = 0.72f),
                                unfocusedPlaceholderColor = secondaryText.copy(alpha = 0.72f),
                            ),
                        )

                        if (mode == PasswordDialogMode.ENCRYPT) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = confirmation,
                                onValueChange = {
                                    confirmation = it
                                    localError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(fieldFill, fieldShape),
                                label = {
                                    Text(
                                        text = stringResource(R.string.password_confirm_label),
                                        color = secondaryText.copy(alpha = 0.82f),
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = passwordPlaceholder,
                                        color = secondaryText.copy(alpha = 0.72f),
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = onSurface,
                                ),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                ),
                                shape = fieldShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = onSurface,
                                    unfocusedTextColor = onSurface,
                                    focusedBorderColor = primary,
                                    unfocusedBorderColor = secondaryText.copy(alpha = 0.58f),
                                    cursorColor = primary,
                                    focusedLabelColor = primary,
                                    unfocusedLabelColor = secondaryText.copy(alpha = 0.82f),
                                    focusedPlaceholderColor = secondaryText.copy(alpha = 0.72f),
                                    unfocusedPlaceholderColor = secondaryText.copy(alpha = 0.72f),
                                ),
                            )
                        }

                        val visibleError = localError ?: errorMessage
                        if (visibleError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = visibleError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = stringResource(R.string.action_cancel),
                                    color = secondaryText,
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    when {
                                        password.isBlank() -> localError = empty
                                        mode == PasswordDialogMode.ENCRYPT &&
                                            password != confirmation -> localError = mismatch
                                        else -> onConfirm(password)
                                    }
                                },
                            ) {
                                Text(confirmLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}
