package com.mercangelsoftware.JustOneList

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mercangelsoftware.JustOneList.data.ListDatabase
import com.mercangelsoftware.JustOneList.ui.JustOneListScreen
import com.mercangelsoftware.JustOneList.ui.JustOneListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = ListDatabase.getInstance(applicationContext).listItemDao()
        val factory = ListViewModelFactory(dao)
        enableEdgeToEdge()
        setContent {
            JustOneListTheme {
                val viewModel: ListViewModel = viewModel(factory = factory)
                JustOneListScreen(viewModel)
            }
        }
    }
}
