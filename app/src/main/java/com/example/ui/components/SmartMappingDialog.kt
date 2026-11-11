package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.StudyUtils
import com.example.ui.theme.BlueBrand
import com.example.ui.theme.EmeraldBrand

enum class MappingType {
    VIDEO_LINKS,
    LECTURE_SHEETS,
    TITLES
}

@Composable
fun SmartMappingDialog(
    type: MappingType,
    subjectName: String,
    chapterName: String,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (rawText: String) -> Unit
) {
    val context = LocalContext.current
    var rawInput by remember { mutableStateOf(initialText) }

    val countText = remember(rawInput, type) {
        when (type) {
            MappingType.TITLES -> {
                val lines = rawInput.lines().map { it.trim() }.filter { it.isNotEmpty() }
                "মোট টাইটেল: ${StudyUtils.toBengaliDigits(lines.size)}টি"
            }
            MappingType.LECTURE_SHEETS -> {
                val detected = StudyUtils.parseSheetEntries(rawInput).size
                "সনাক্তকৃত শিট/স্ট্যাটাস: ${StudyUtils.toBengaliDigits(detected)}টি"
            }
            MappingType.VIDEO_LINKS -> {
                val detected = StudyUtils.extractUrls(rawInput).size
                "সনাক্তকৃত লিঙ্ক: ${StudyUtils.toBengaliDigits(detected)}টি"
            }
        }
    }

    val title = when (type) {
        MappingType.VIDEO_LINKS -> "স্মার্ট লিঙ্ক ম্যাপিং (অধ্যায়)"
        MappingType.LECTURE_SHEETS -> "স্মার্ট লিঙ্ক ম্যাপিং Lecture Sheet"
        MappingType.TITLES -> "টাইটেল ম্যাপিং (Lecture Tag)"
    }

    val hintText = when (type) {
        MappingType.VIDEO_LINKS -> "প্রতিটি লাইনে বা টেক্সটের সাথে YouTube/Class Video লিঙ্ক পেস্ট করুন। অ্যাপ স্বয়ংক্রিয়ভাবে সঠিক লিঙ্কগুলো সনাক্ত করে লেকচার ১, ২, ৩... এ যুক্ত করবে এবং আসল YouTube টাইটেল আনবে।"
        MappingType.LECTURE_SHEETS -> "প্রতিটি লাইনে লেকচার শিট, ক্লাস নোট বা Google Drive PDF লিঙ্ক পেস্ট করুন। যে ক্লাসে শিট নেই সেখানে N/A লিখুন (যেমন: লিঙ্ক অথবা N/A)। অ্যাপ ক্রমানুসারে নীল Sheet ও লাল N/A সেট করবে।"
        MappingType.TITLES -> "প্রতিটি লাইনে লেকচারের ইংরেজি সাব-টাইটেল বা ট্যাগ লিখুন (যেমন: Lecture:01, Lecture:02, Lecture:02.2, Lecture:03...)। অ্যাপ ক্রমানুসারে মূল টাইটেলের নিচে এগুলো সেট করবে।"
    }

    val placeholderText = when (type) {
        MappingType.VIDEO_LINKS -> "https://youtu.be/example1\nhttps://youtu.be/example2"
        MappingType.LECTURE_SHEETS -> "https://drive.google.com/file/d/example1\nhttps://drive.google.com/file/d/example2\nN/A\nhttps://drive.google.com/file/d/example3"
        MappingType.TITLES -> "Lecture:01\nLecture:02\nLecture:02.2\nLecture:03\nLecture:04\nLecture:04.2"
    }

    val themeColor = when (type) {
        MappingType.VIDEO_LINKS -> BlueBrand
        MappingType.LECTURE_SHEETS -> EmeraldBrand
        MappingType.TITLES -> Color(0xFF8B5CF6) // Purple / Violet
    }

    val iconVector = when (type) {
        MappingType.VIDEO_LINKS -> Icons.Default.Link
        MappingType.LECTURE_SHEETS -> Icons.Default.Description
        MappingType.TITLES -> Icons.Default.EditNote
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = themeColor
                    )
                    Column {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$subjectName • $chapterName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = hintText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    placeholder = { Text(placeholderText, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("bulk_links_textarea"),
                    maxLines = 15
                )

                // Counter and Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColor
                    )

                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Action Buttons Row: [link set] (for links) + [সেভ করুন]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (type != MappingType.TITLES) {
                        // "copy" button (Requirement 4)
                        OutlinedButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("mapped_links", rawInput)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "সকল লিঙ্ক কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "কপি করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_copy_links")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("copy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // "link set" button (Requirement 4)
                        OutlinedButton(
                            onClick = {
                                if (type == MappingType.LECTURE_SHEETS) {
                                    rawInput = StudyUtils.formatSheetMappingText(rawInput)
                                } else {
                                    val formatted = rawInput
                                        .replace(Regex("(?<!^)(?<!\n)(https?://)"), "\n$1")
                                        .lines()
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .joinToString("\n")
                                    rawInput = formatted
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_link_set")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("link set", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = { onSave(rawInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_bulk_links_btn")
                    ) {
                        Text(
                            text = if (type == MappingType.TITLES) "টাইটেল সেভ করুন" else "লিঙ্ক সেভ করুন",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
