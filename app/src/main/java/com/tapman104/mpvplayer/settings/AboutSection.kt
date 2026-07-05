package com.tapman104.mpvplayer.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutSection() {
    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = "About",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )

        // App Name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("App Name", color = Color.White, fontSize = 15.sp)
            Text("Potato Player", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

        // Version
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Version", color = Color.White, fontSize = 15.sp)
            Text("1.0", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

        // MPV Library
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MPV Library", color = Color.White, fontSize = 15.sp)
            Text("libmpv", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

        // Open Source
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Open Source", color = Color.White, fontSize = 15.sp)
            Text("github.com/tapman104", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
    }
}
