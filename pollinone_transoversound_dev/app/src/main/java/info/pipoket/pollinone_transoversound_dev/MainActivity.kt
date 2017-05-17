package info.pipoket.pollinone_transoversound_dev

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

class MainActivity : AppCompatActivity() {

    val dtmfReceiver = AudioDTMFReceiver()

    companion object {
        val PERM_REQUEST_CODE_RECORD = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        dtmfReceiver.start()
    }

    override fun onResume() {
        super.onResume()
        dtmfReceiver.start()
    }

    override fun onPause() {
        super.onPause()
        dtmfReceiver.close()
    }

    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            dtmfReceiver.notifyPermissionGrant()
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

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            return

        dtmfReceiver.notifyPermissionGrant()
    }
}
