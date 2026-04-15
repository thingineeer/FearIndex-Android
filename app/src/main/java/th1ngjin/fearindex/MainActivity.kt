package th1ngjin.fearindex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import th1ngjin.fearindex.presentation.navigation.FearIndexNavHost
import th1ngjin.fearindex.presentation.theme.FearIndexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FearIndexTheme {
                FearIndexNavHost()
            }
        }
    }
}
