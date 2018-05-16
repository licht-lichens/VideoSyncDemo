package com.lichens.licht.videosyncdemo

import android.app.Application
import android.widget.Toast
import com.ksyun.media.player.d.d.s
import com.lichens.licht.videosyncdemo.mqtt.MqttManager
import com.lichens.licht.videosyncdemo.ui.MainActivity.mqttConnectState

/**
 * Created by licht on 2018/5/15.
 */
class MyApp : Application(){

    private val URL = "tcp://192.168.3.116:1883"
    private val userName = "userName"
    private val password = "password"
    private val clientId = "clientId_01"


    override fun onCreate() {
        super.onCreate()
        //连接mqtt
        connectMqtt()
    }

    /**
     * 连接mqtt 注册频道
     */
    private fun connectMqtt() {
            //如果连接成功  则注册test频道
            if (MqttManager.getInstance().creatConnect(URL, userName, password, clientId) && MqttManager.getInstance().subscribe("test", 2)) {
                mqttConnectState = true
            } else {
                Toast.makeText(this, "服务器连接异常", Toast.LENGTH_SHORT).show()
            }
    }
}