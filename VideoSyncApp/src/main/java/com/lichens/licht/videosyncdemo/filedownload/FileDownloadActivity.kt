package com.lichens.licht.videosyncdemo.filedownload

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.AppCompatRadioButton
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.lichens.licht.videosyncdemo.R
import com.lichens.licht.videosyncdemo.queue.QueueController
import com.lichens.licht.videosyncdemo.queue.QueueRecyclerAdapter
import com.lichens.licht.videosyncdemo.utils.LogUtil
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause

class FileDownloadActivity : AppCompatActivity() {

    private var controller: QueueController? = null
    private var adapter: QueueRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_download)
        checkPermission()
        initQueueActivity(findViewById(R.id.actionView), findViewById<View>(R.id.actionTv) as TextView,
                findViewById<View>(R.id.serialRb) as AppCompatRadioButton,
                findViewById<View>(R.id.parallelRb) as AppCompatRadioButton,
                findViewById<View>(R.id.recyclerView) as RecyclerView,
                findViewById<View>(R.id.deleteActionView) as CardView, findViewById(R.id.deleteActionTv))
    }

    override fun onDestroy() {
        super.onDestroy()
        this.controller!!.stop()
    }

    private fun initQueueActivity(actionView: View, actionTv: TextView,
                                  serialRb: AppCompatRadioButton,
                                  parallelRb: AppCompatRadioButton,
                                  recyclerView: RecyclerView,
                                  deleteActionView: CardView, deleteActionTv: View) {
        //初始化控制器
        initController(actionView, actionTv, serialRb, parallelRb, deleteActionView, deleteActionTv)
        //初始化recyclerview
        initRecyclerView(recyclerView)
        //初始化action
        initAction(actionView, actionTv, serialRb, parallelRb, deleteActionView, deleteActionTv)
    }

    private fun initController(actionView: View, actionTv: TextView,
                               serialRb: AppCompatRadioButton,
                               parallelRb: AppCompatRadioButton,
                               deleteActionView: CardView, deleteActionTv: View) {

        val controller = QueueController()
        this.controller = controller
        controller.initTasks(this, object : DownloadContextListener {
            override fun taskEnd(context: DownloadContext, task: DownloadTask,
                                 cause: EndCause,
                                 realCause: Exception?,
                                 remainCount: Int) {
            }

            override fun queueEnd(context: DownloadContext) {
                actionView.tag = null
                actionTv.setText(R.string.start)
                // to cancel
                controller.stop()

                serialRb.isEnabled = true
                parallelRb.isEnabled = true

                deleteActionView.setEnabled(true)
                deleteActionView.setCardElevation(deleteActionView.getTag() as Float)
                deleteActionTv.isEnabled = true

                adapter!!.notifyDataSetChanged()
            }
        })

    }

    private fun initRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        val adapter = QueueRecyclerAdapter(controller!!)
        this.adapter = adapter
        recyclerView.setAdapter(adapter)
    }

    private fun initAction(actionView: View, actionTv: TextView,
                           serialRb: AppCompatRadioButton,
                           parallelRb: AppCompatRadioButton,
                           deleteActionView: CardView, deleteActionTv: View) {
        deleteActionView.setOnClickListener(View.OnClickListener {
            controller!!.deleteFiles()
            adapter!!.notifyDataSetChanged()
        })

        actionTv.setText(R.string.start)
        actionView.setOnClickListener { v ->
            val started = v.tag != null

            if (started) {
                controller!!.stop()
            } else {
                v.tag = Any()
                actionTv.setText(R.string.cancel)

                // to start
                controller!!.start(serialRb.isChecked)
                adapter!!.notifyDataSetChanged()

                serialRb.isEnabled = false
                parallelRb.isEnabled = false
                deleteActionView.setEnabled(false)
                deleteActionView.setTag(deleteActionView.getCardElevation())
                deleteActionView.setCardElevation(0f)
                deleteActionTv.isEnabled = false
            }
        }
    }

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
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show()
            LogUtil.e("已经授权")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtil.e("授权成功")
    }

}
