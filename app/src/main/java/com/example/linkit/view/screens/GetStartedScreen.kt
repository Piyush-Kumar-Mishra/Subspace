package com.example.linkit.view.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkit.R
import com.example.linkit.ui.theme.AntonFontFamily
import com.example.linkit.ui.theme.bcg
import com.example.linkit.ui.theme.bcg2

@Composable
fun GetStartedScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bcg2)
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box {
                Text(
                    text = "Welcome to LinkIt",
                    fontFamily = AntonFontFamily,
                    fontSize = 50.sp,
                    color = Color.Black,
                    modifier = Modifier.offset(x = (-4).dp, y = 7.dp)
                )
                Text(
                    text = "Welcome to LinkIt",
                    fontFamily = AntonFontFamily,
                    fontSize = 50.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp)) 
            
            Button(
                onClick = onGetStarted,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .height(60.dp)
                    .padding(horizontal = 24.dp) 
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Get Started",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }


        Image(
            painter = painterResource(id = R.drawable.img3),
            contentDescription = null,
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.BottomEnd)
        )

        Image(
            painter = painterResource(id = R.drawable.img4),
            contentDescription = null,
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.Center)
                .rotate(60f)
                .offset(x = (-90).dp)
        )

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GetStartedScreenPreview() {
    GetStartedScreen(onGetStarted = {})
}