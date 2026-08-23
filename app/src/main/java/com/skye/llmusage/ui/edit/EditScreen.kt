package com.skye.llmusage.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.skye.llmusage.R
import com.skye.llmusage.data.AccountType
import com.skye.llmusage.data.Provider

/** 添加/编辑账户:类型 → 提供商 → 名称/Key */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    initialAccountId: Long?,
    initialName: String?,
    initialProvider: Provider?,
    initialKey: String?,
    onBack: () -> Unit,
    onSave: (Long?, String, Provider, String) -> Unit,
) {
    var type by rememberSaveable {
        mutableStateOf((initialProvider ?: Provider.GLM_CN).type.name)
    }
    var provider by rememberSaveable {
        mutableStateOf((initialProvider ?: Provider.GLM_CN).name)
    }
    var name by rememberSaveable { mutableStateOf(initialName ?: "") }
    var apiKey by rememberSaveable { mutableStateOf(initialKey ?: "") }
    var keyError by rememberSaveable { mutableStateOf(false) }

    // 切换类型时,若当前提供商不属于该类型则重置为该类型第一个提供商
    LaunchedEffect(type) {
        val current = Provider.valueOf(provider)
        if (current.type.name != type) {
            provider = Provider.entries.first { it.type.name == type }.name
        }
    }

    val isEdit = initialAccountId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEdit) R.string.edit_account else R.string.add_account,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 类型选择
            Text("账户类型", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AccountType.entries.forEachIndexed { index, t ->
                    SegmentedButton(
                        selected = type == t.name,
                        onClick = { type = t.name },
                        shape = SegmentedButtonDefaults.itemShape(index, AccountType.entries.size),
                    ) {
                        Text(
                            when (t) {
                                AccountType.CODING_PLAN -> stringResource(R.string.type_coding_plan)
                                AccountType.PAYG -> stringResource(R.string.type_payg)
                            },
                        )
                    }
                }
            }

            // 提供商选择(数量多,FlowRow 自动换行)
            Text("提供商", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val providers = Provider.entries.filter { it.type.name == type }
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                providers.forEach { p ->
                    FilterChip(
                        selected = provider == p.name,
                        onClick = { provider = p.name },
                        label = { Text(p.displayName) },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name)) },
                supportingText = { Text(stringResource(R.string.field_name_support)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    keyError = false
                },
                label = { Text(stringResource(R.string.field_api_key)) },
                supportingText = {
                    Text(
                        when (Provider.valueOf(provider)) {
                            Provider.DEEPSEEK -> stringResource(R.string.hint_deepseek_key)
                            Provider.KIMI_CODING -> stringResource(R.string.hint_kimi_key)
                            Provider.MINIMAX_CN, Provider.MINIMAX_INTL -> stringResource(R.string.hint_minimax_key)
                            else -> stringResource(R.string.hint_glm_key)
                        },
                    )
                },
                isError = keyError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    if (apiKey.isBlank()) {
                        keyError = true
                    } else {
                        onSave(initialAccountId, name, Provider.valueOf(provider), apiKey)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_and_refresh))
            }
        }
    }
}
