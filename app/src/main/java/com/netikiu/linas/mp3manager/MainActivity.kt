package com.netikiu.linas.mp3manager

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat.startActivity
import com.netikiu.linas.mp3manager.ui.theme.Mp3managerTheme
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Environment.isExternalStorageManager()) {
            //todo when permission is granted
        } else {
            //request for the permission
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);

            val uri = Uri.fromParts("package", packageName, null);
            intent.setData(uri);
            startActivity(intent);
        }


        val files = readFilesFromDownloadDirectory()
        val metadataList = retrieveMetadata(files)

        val metadataState = mutableStateListOf<MusicMetaData.F>()
        metadataState.addAll(metadataList);

        enableEdgeToEdge()
        setContent {
            Mp3managerTheme() {

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(
                        title = { Text( "MP3 manager")},
                        actions = {IconButton(onClick = {
                            val fls = readFilesFromDownloadDirectory()
                            val m = retrieveMetadata(fls)
                            metadataState.clear();
                            metadataState.addAll(m);
                        }) {
                            Icon(imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",)
                        } }
                    )
                    },
                    modifier = Modifier.wrapContentHeight()
                ) {

                    innerPadding ->
                    Greeting(
                        name = "Androidas",
                        mlist = metadataState,
                        modifier = Modifier.padding(innerPadding),
                        c = this
                    )
                }
            }
        }
    }
}


fun retrieveMetadata(files : List<File>) : List<MusicMetaData.F> {

    Log.e("mp3", "files: ${files.size}")
    for (file in files) {
        // Do something with the file
        Log.e("mp3", "onCreate: $file")
    }

    val ffiles = files.filter { it.isFile && it.extension.uppercase() == "MP3" }
    val mlist = ffiles.map {
        MusicMetaData.extractData(it)
    }.sortedByDescending { it.rating }

    return mlist;

}

@Composable
fun Greeting(mlist: SnapshotStateList<MusicMetaData.F>, name: String, modifier: Modifier = Modifier, c: Context) {


       remember {
           mlist
       }

        Column(modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            mlist.forEach { metaData ->

                val color1 =
                    if (metaData.rating >= 3) Color(176,255,157)
                    else if (metaData.rating > -1)  Color(251, 234, 234)
                    else  Color(239, 242, 240)

                Log.e("mp3", "Adding file ${metaData.f.name}")

                Card(
                colors = CardDefaults.cardColors(
                    containerColor = color1
                ),
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(0.dp)
                    .fillMaxWidth()) {

                Text(
                    text = "${metaData.artist} - ${metaData.title}",
                    fontWeight = FontWeight.Bold,
                    softWrap = true
                )

                Text(
                    text = "${metaData.f.name} Rating: ${metaData.rating}",
                    softWrap = true
                )

                val s = URLEncoder.encode("${metaData.artist} - ${metaData.title}", StandardCharsets.UTF_8.toString())

                Row() {
                    Button(onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$s"))
                        c.startActivity(browserIntent)
                    }) {
                        Text("Spotify")
                    }
                    Button(onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$s"))
                        c.startActivity(browserIntent)
                    }) {
                        Text("YT music")
                    }
                    Button(onClick = {
                        val clipboard = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("song info", "${metaData.artist} - ${metaData.title}\n${metaData.f.absolutePath}")
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Text("Copy")
                    }
                    Button(
                        enabled = metaData.rating > 0 && metaData.rating < 3,
                        onClick = {
                            metaData.f.delete()
                            mlist.remove(metaData)
                        }) {
                        Text("Del")
                    }

                }
            }

        }

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {

    val files = readFilesFromLocalDirectory()
    val z = retrieveMetadata(files )

    val zz = remember { mutableStateListOf<MusicMetaData.F>()}
    zz.addAll(z);

    Mp3managerTheme {
        Greeting(mlist = zz, name="test", c=Activity())
    }

    zz.removeAt(1)
}

fun readFilesFromDownloadDirectory(): List<File> {
    val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val d = File(downloadDirectory.absolutePath + "/sync/phone-media/rate")
    return d.walkTopDown().toList()
}


fun readFilesFromLocalDirectory(): List<File> {
    val d = File("/home/bro1/syncthing-data/main-phone-media/rate/")
    return d.walkTopDown().toList()
}