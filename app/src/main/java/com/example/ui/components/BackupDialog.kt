package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.StudyUtils
import com.example.ui.theme.EmeraldBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupDialog(
    totalSubjects: Int,
    completedLectures: Int,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showResetConfirm by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }

    // SAF Document Creator for direct .json file download / save
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isExporting = true
            coroutineScope.launch {
                try {
                    val json = withContext(Dispatchers.IO) { onExportJson() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    exportSuccess = true
                    Toast.makeText(context, "ব্যাকআপ ফাইল (.json) সফলভাবে ডাউনলোড / সেভ হয়েছে!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "ফাইল সংরক্ষণ করতে ব্যর্থ হয়েছে: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    // SAF Document Picker for direct .json file upload / restore without code pasting
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            coroutineScope.launch {
                try {
                    val jsonText = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        }
                    } ?: ""
                    if (jsonText.isNotBlank()) {
                        val success = withContext(Dispatchers.IO) { onImportJson(jsonText) }
                        if (success) {
                            Toast.makeText(context, "ব্যাকআপ ফাইল থেকে ডেটা সফলভাবে রিস্টোর হয়েছে!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "ফাইলটি বৈধ HSC স্টাডি ট্র্যাকার ব্যাকআপ নয়!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "নির্বাচিত ফাইলটি খালি!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "ফাইল পড়তে ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = EmeraldBrand
                    )
                    Column {
                        Text(
                            text = "ডেটা ব্যাকআপ ও সিঙ্ক",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "লোকাল স্টোরেজ ও ব্যাকআপ ফাইল",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stats Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = StudyUtils.toBengaliDigits(totalSubjects),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "মোট বিষয়",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = StudyUtils.toBengaliDigits(completedLectures),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldBrand
                            )
                            Text(
                                text = "সম্পন্ন ক্লাস",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Export Data Button (.json File Download)
                Button(
                    onClick = {
                        val fileName = "hsc_study_tracker_backup_${System.currentTimeMillis() / 1000}.json"
                        createDocumentLauncher.launch(fileName)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("export_backup_btn")
                ) {
                    Icon(
                        imageVector = if (exportSuccess) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExporting) "ফাইল তৈরি হচ্ছে..." else if (exportSuccess) "ব্যাকআপ ফাইল সংরক্ষিত হয়েছে ✓" else "ব্যাকআপ ডাটা ডাউনলোড করুন (.json)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 2. Import / Restore Data Button (.json File Upload - No code paste needed!)
                OutlinedButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("import_backup_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isImporting) "ফাইল রিস্টোর হচ্ছে..." else "ব্যাকআপ থেকে রিস্টোর করুন (.json ফাইল আপলোড)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3. Reset Defaults
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ডিফল্ট ডেটা রিস্টোর",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "মূল বিষয় ও অধ্যায়গুলোতে ফিরতে",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("রিসেট", fontSize = 11.sp)
                    }
                }

                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বন্ধ করুন", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("ডেটা রিসেট নিশ্চিতকরণ") },
            text = { Text("আপনি কি সমস্ত ডেটা রিসেট করে মূল বিষয় ও অধ্যায়গুলোতে ফিরে যেতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetDefaults()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("হ্যাঁ, রিসেট করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
