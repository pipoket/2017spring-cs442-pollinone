package info.pipoket.pollinone_transoversound_dev

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var dtmfReceiver: PollInOneToAReceiver? = null

    companion object {
        val PERM_REQUEST_CODE_RECORD = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnListen.setOnClickListener {
            btnListen.isEnabled = false
            btnCancel.isEnabled = true
            txtRawData.text = "----"
            txtIntData.text = "----"
            dtmfReceiver?.start()
        }
        btnCancel.setOnClickListener {
            dtmfReceiver?.close()
        }

        resetReceiver()
    }

    override fun onResume() {
        super.onResume()
        resetReceiver()
    }

    override fun onPause() {
        super.onPause()
        dtmfReceiver?.close()
    }

    fun resetReceiver() {
        dtmfReceiver = PollInOneToAReceiver(
                { state: PollInOneToAReceiver.ReceiverState -> stateUpdated(state) },
                { result: String -> dataReceived(result) },
                { errorCode: PollInOneToAReceiver.ErrorCode -> receiveError(errorCode) }
        )
        btnListen.isEnabled = true
        btnCancel.isEnabled = false
        txtStatus.text = "Initialized"
        checkPermission()
    }

    fun stateUpdated(state: PollInOneToAReceiver.ReceiverState) {
        val mainHandler = Handler(this.mainLooper)
        mainHandler.post {
            when (state) {
                PollInOneToAReceiver.ReceiverState.INIT -> txtStatus.text = "Initialized"
                PollInOneToAReceiver.ReceiverState.LISTEN -> txtStatus.text = "Listening"
                PollInOneToAReceiver.ReceiverState.PARSE_DATA -> txtStatus.text = "Parsing"
                PollInOneToAReceiver.ReceiverState.RECEIVE_DONE -> txtStatus.text = "Got Data!"
                else -> txtStatus.text = "Unknown State"
            }
        }
    }

    fun dataReceived(result: String) {
        Log.i("MainActivity", "Data received $result")

        val mainHandler = Handler(this.mainLooper)
        mainHandler.post {
            txtRawData.text = "0x$result"
            txtIntData.text = result.toInt(16).toString()
            resetReceiver()
            if (!btnListen.isEnabled) {
                btnListen.isEnabled = true
                btnCancel.isEnabled = false
            }
        }
    }

    fun receiveError(errorCode: PollInOneToAReceiver.ErrorCode) {
        Log.i("MainActivity", "Error received $errorCode")

        val mainHandler = Handler(this.mainLooper)
        mainHandler.post {
            resetReceiver()
            if (!btnListen.isEnabled) {
                btnListen.isEnabled = true
                btnCancel.isEnabled = false
            }
        }
    }

    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            dtmfReceiver?.notifyPermissionGrant()
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO), PERM_REQUEST_CODE_RECORD)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != PERM_REQUEST_CODE_RECORD)
            return

        if (grantResults.isEmpty())
            return

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            return

        dtmfReceiver?.notifyPermissionGrant()
    }
}
