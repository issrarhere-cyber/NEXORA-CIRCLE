package com.nexora.circle.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Firebase Auth UID of the founder account.
 * Find it in: Firebase Console -> Authentication -> Users -> "User UID".
 * The badge is decided by this UID (not by name or handle), so nobody else can fake it.
 */
const val FOUNDER_UID = "UEeniKJjVGOBnKvqkQ17OHE5V6u2"

val FounderGold = Color(0xFFFFC83D)

fun isFounder(uid: String): Boolean = uid.isNotBlank() && uid == FOUNDER_UID

/** Gold verified seal, shown next to the founder's name. */
@Composable
fun FounderBadge(size: Dp = 18.dp) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Founder, verified",
        tint = FounderGold,
        modifier = Modifier.size(size),
    )
}

/** Gold "Founder" pill for the profile page. */
@Composable
fun FounderTag() {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, FounderGold.copy(alpha = 0.6f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FounderBadge(size = 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            "Founder",
            color = FounderGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
