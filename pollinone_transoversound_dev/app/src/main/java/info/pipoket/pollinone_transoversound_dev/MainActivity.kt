package info.pipoket.pollinone_transoversound_dev

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    val dtmfReceiver = AudioDTMFReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dtmfReceiver.start()
    }

    override fun onResume() {
        dtmfReceiver.start()
    }

    override fun onPause() {
        dtmfReceiver.close()
    }
}
