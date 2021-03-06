package com.lichens.licht.videosyncdemo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.text.TextUtils
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.lichens.licht.videosyncdemo.mqtt.MqttManager
import com.lichens.licht.videosyncdemo.utils.LogUtils
import com.lichens.licht.videosyncdemo.utils.SharePrefUtil
import org.greenrobot.eventbus.EventBus

abstract class BaseActivity : AppCompatActivity() {

    private var TAG = "Activity"
    private val URL = "tcp://192.168.3.116:1883"
    private val userName = "userName"
    private val password = "password"
    private var clientId = "clientId_02"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        // 设置全屏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        //检查文件权限
        checkPermission()
        if (!TextUtils.isEmpty(SharePrefUtil.getString("CLIENTID")))
            clientId = SharePrefUtil.getString("CLIENTID")
    }

    //权限检查
    private fun checkPermission() {
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show()
            }
            //申请权限
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)

        } else {
            LogUtils.e(TAG, "checkPermission: 已经授权！")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtils.e(TAG, "授权成功!" + requestCode)
    }


    override fun onResume() {
        super.onResume()

        //连接mqtt
        connectMqtt()
        //注册eventbus
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        //解注册eventbus
        EventBus.getDefault().unregister(this)
    }

    /**
     * 连接mqtt 注册频道
     */
    private fun connectMqtt() {
        //如果连接成功  则注册test频道
        Thread(Runnable {
            if (MqttManager.getInstance().creatConnect(URL, userName, password, clientId) && MqttManager.getInstance().subscribe("test", 2)) {
                mqttConnectSuccess()
            } else {
                mqttConnectError()
            }
        }).start()
    }

    abstract fun mqttConnectSuccess()
    abstract fun mqttConnectError()
}
