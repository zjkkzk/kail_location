package com.zcshou.gogogo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zcshou.gogogo.R

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onAgreementClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.welcome),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .width(200.dp)
            ) {
                Text(text = stringResource(id = R.string.welcome_btn_txt))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = Color.White
                    )
                )

                val agreementText = stringResource(id = R.string.app_agreement_privacy)
                val agreementPart = "《用户协议》"
                val privacyPart = "《隐私政策》"

                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.White)) {
                        append("已阅读")
                    }
                    
                    pushStringAnnotation(tag = "agreement", annotation = "agreement")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(agreementPart)
                    }
                    pop()

                    withStyle(style = SpanStyle(color = Color.White)) {
                        append("和")
                    }

                    pushStringAnnotation(tag = "privacy", annotation = "privacy")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(privacyPart)
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "agreement", start = offset, end = offset)
                            .firstOrNull()?.let {
                                onAgreementClick()
                            }
                        annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                            .firstOrNull()?.let {
                                onPrivacyClick()
                            }
                    }
                )
            }
        }
    }
}

@Composable
fun AgreementDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    onAgree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { 
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = content)
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(text = stringResource(id = R.string.app_btn_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.app_btn_disagree))
            }
        }
    )
}
