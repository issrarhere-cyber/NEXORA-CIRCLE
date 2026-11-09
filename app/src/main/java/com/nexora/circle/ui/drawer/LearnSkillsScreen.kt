package com.nexora.circle.ui.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.circle.ui.theme.NexoraColors

private data class Lesson(val title: String, val minutes: Int)

private data class Skill(
    val id: String,
    val emoji: String,
    val name: String,
    val blurb: String,
    val lessons: List<Lesson>,
)

private val SKILLS: List<Skill> = listOf(
    Skill(
        id = "coding",
        emoji = "💻",
        name = "Coding",
        blurb = "Learn to think like a programmer and build small apps.",
        lessons = listOf(
            Lesson("What is programming? Your first program", 10),
            Lesson("Variables and data types", 12),
            Lesson("Conditions: if / else", 11),
            Lesson("Loops: repeat things easily", 13),
            Lesson("Functions: reusable blocks of code", 15),
            Lesson("Lists and simple data structures", 14),
            Lesson("Debugging: finding and fixing errors", 12),
            Lesson("Build a small project from scratch", 25),
        ),
    ),
    Skill(
        id = "designing",
        emoji = "🎨",
        name = "Designing",
        blurb = "Colour, type and layout for apps, posters and logos.",
        lessons = listOf(
            Lesson("Design basics: balance, contrast, hierarchy", 10),
            Lesson("Colour theory for beginners", 12),
            Lesson("Typography: choosing and pairing fonts", 11),
            Lesson("Layout, spacing and grids", 13),
            Lesson("Designing a simple logo", 16),
            Lesson("App screen design (UI) basics", 18),
            Lesson("Making a poster in Canva", 14),
            Lesson("Building a design portfolio", 12),
        ),
    ),
    Skill(
        id = "speaking",
        emoji = "🎤",
        name = "Public Speaking",
        blurb = "Speak clearly and confidently in front of any audience.",
        lessons = listOf(
            Lesson("Overcoming stage fear", 10),
            Lesson("Structure of a good speech", 12),
            Lesson("Voice, pace and pauses", 11),
            Lesson("Body language and eye contact", 10),
            Lesson("Storytelling that holds attention", 14),
            Lesson("Handling questions and interruptions", 11),
            Lesson("Practice plan: 7 days to a better speech", 9),
        ),
    ),
    Skill(
        id = "english",
        emoji = "🗣️",
        name = "English",
        blurb = "Speak, write and understand English with confidence.",
        lessons = listOf(
            Lesson("Daily conversation phrases", 10),
            Lesson("Grammar: simple tenses", 14),
            Lesson("Grammar: continuous and perfect tenses", 15),
            Lesson("Vocabulary: 50 useful everyday words", 12),
            Lesson("Pronunciation and clear accent", 13),
            Lesson("Writing a polite email", 11),
            Lesson("Listening practice with short stories", 15),
            Lesson("Speaking practice: introduce yourself", 9),
        ),
    ),
    Skill(
        id = "ai",
        emoji = "🤖",
        name = "AI",
        blurb = "Understand AI and use it safely for study and work.",
        lessons = listOf(
            Lesson("What is AI? Simple explanation", 9),
            Lesson("How chatbots and language models work", 13),
            Lesson("Writing good prompts", 12),
            Lesson("Using AI for study and research", 14),
            Lesson("AI tools for work and creativity", 15),
            Lesson("Checking AI answers: facts and mistakes", 10),
            Lesson("AI safety, privacy and ethics", 11),
        ),
    ),
    Skill(
        id = "business",
        emoji = "📈",
        name = "Business",
        blurb = "Start small, think like an owner, grow step by step.",
        lessons = listOf(
            Lesson("Finding a business idea that solves a problem", 12),
            Lesson("Knowing your customer and market", 13),
            Lesson("Simple budget, costs and profit", 15),
            Lesson("Marketing basics for a small business", 14),
            Lesson("Selling and negotiation", 13),
            Lesson("Managing time, money and a small team", 12),
            Lesson("Writing a one-page business plan", 16),
        ),
    ),
)

private const val LEARN_PREFS = "nexora_learn"
private const val LEARN_KEY = "done"

private fun loadDone(context: Context): Set<String> {
    val saved = context.getSharedPreferences(LEARN_PREFS, Context.MODE_PRIVATE)
        .getStringSet(LEARN_KEY, null)
    return saved?.toSet() ?: emptySet()
}

private fun saveDone(context: Context, done: Set<String>) {
    context.getSharedPreferences(LEARN_PREFS, Context.MODE_PRIVATE)
        .edit().putStringSet(LEARN_KEY, HashSet(done)).apply()
}

private fun doneKey(skillId: String, index: Int) = "$skillId:$index"

@Composable
fun LearnSkillsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var done by remember { mutableStateOf(loadDone(context)) }
    var openSkillId by remember { mutableStateOf("") }

    val skill = SKILLS.firstOrNull { it.id == openSkillId }

    if (skill == null) {
        DrawerScreenScaffold(title = "Learn Skills", onBack = onBack) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Pick a skill and finish its lessons one by one.",
                    color = NexoraColors.Muted.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                )
                SKILLS.forEach { s ->
                    val doneCount = s.lessons.indices.count { done.contains(doneKey(s.id, it)) }
                    val fraction = if (s.lessons.isEmpty()) 0f else doneCount.toFloat() / s.lessons.size
                    NxCard(onClick = { openSkillId = s.id }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.emoji, fontSize = 34.sp)
                            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(
                                    s.name,
                                    color = NexoraColors.StarWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    s.blurb,
                                    color = NexoraColors.Muted.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                )
                            }
                            Text("›", color = NexoraColors.Muted, fontSize = 26.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                            color = NexoraColors.PlasmaCyan,
                            trackColor = NexoraColors.Divider,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$doneCount of ${s.lessons.size} lessons done",
                            color = NexoraColors.Muted.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    } else {
        val doneCount = skill.lessons.indices.count { done.contains(doneKey(skill.id, it)) }
        DrawerScreenScaffold(title = "${skill.emoji} ${skill.name}", onBack = { openSkillId = "" }) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "$doneCount of ${skill.lessons.size} lessons done",
                    color = NexoraColors.PlasmaCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = {
                        if (skill.lessons.isEmpty()) 0f else doneCount.toFloat() / skill.lessons.size
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = NexoraColors.PlasmaCyan,
                    trackColor = NexoraColors.Divider,
                )
                skill.lessons.forEachIndexed { index, lesson ->
                    val key = doneKey(skill.id, index)
                    val isDone = done.contains(key)
                    NxCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDone) NexoraColors.PlasmaCyan else NexoraColors.Divider,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (isDone) "✓" else (index + 1).toString(),
                                    color = if (isDone) NexoraColors.SpaceBlack else NexoraColors.StarWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lesson.title,
                                    color = NexoraColors.StarWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Video lesson  ·  ${lesson.minutes} min",
                                    color = NexoraColors.Muted.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { watchLesson(context, skill.name, lesson.title) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Watch") }
                            if (isDone) {
                                Button(
                                    onClick = {
                                        val next = done - key
                                        done = next
                                        saveDone(context, next)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Done ✓") }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        val next = done + key
                                        done = next
                                        saveDone(context, next)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Mark as Done") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Opens a YouTube search for the lesson, so every lesson has real videos to watch. */
private fun watchLesson(context: Context, skillName: String, lessonTitle: String) {
    val query = Uri.encode("$skillName $lessonTitle tutorial for beginners")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open YouTube", Toast.LENGTH_SHORT).show()
    }
}
