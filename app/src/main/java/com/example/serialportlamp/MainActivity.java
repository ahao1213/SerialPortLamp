package com.example.serialportlamp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    public boolean isOpen;
    private int retval;
    private Context mContext;
    private ReadThread mReadThread;

    public final int baudRate = 2400;
    public byte baudRate_byte;
    public byte stopBit = 1;
    public byte dataBit = 8;
    public byte parity = 0;
    public byte flowControl = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;


        initCH34();

        findViewById(R.id.main_send_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] close = new byte[7];
                close[0] = (byte) 0xEB;
                close[1] = (byte) 0xF3;
                close[2] = (byte) 0x3F;
                close[3] = (byte) 0x00;
                close[4] = (byte) 0xD4;
                close[5] = (byte) 0x03;
                close[6] = (byte) 0xEB;
                write(close);
            }
        });

    }

    private void initCH34() {
        LampApplication.driver = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), mContext,
                ACTION_USB_PERMISSION);

        if (!LampApplication.driver.UsbFeatureSupported())// 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
//                                    finish();
                                }
                            }).setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_BACK:
                                    finish();
                                    break;
                            }
                            return false;
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } else {
            open();
        }
    }

    private void open() {
        if (!isOpen) {
            retval = LampApplication.driver.ResumeUsbList();
            if (retval == -1)// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
            {
                Dialog dialog = new AlertDialog.Builder(mContext)
                        .setTitle("提示")
                        .setMessage("设备未连接，请连接后重试！")
                        .setPositiveButton("确认",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface arg0,
                                                        int arg1) {
//                                        finish();
                                    }
                                }).setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                switch (keyCode) {
                                    case KeyEvent.KEYCODE_BACK:
                                        finish();
                                        break;
                                }
                                return false;
                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                LampApplication.driver.CloseDevice();
            }
        } else if (retval == 0) {
            if (!LampApplication.driver.UartInit()) {
                Toast.makeText(mContext, "设备初始化失败！", Toast.LENGTH_SHORT).show();
                return;
            }
            isOpen = true;
            mReadThread = new ReadThread();
            mReadThread.start();//开启线程读取串口数据


            if (LampApplication.driver.SetConfig(baudRate, dataBit, stopBit, parity,//配置串口波特率，函数说明可参照编程手册
                    flowControl)) {
                Toast.makeText(mContext, "打开设备成功!",
                        Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(mContext, "打开设备失败!",
                        Toast.LENGTH_SHORT).show();
            }


        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//            builder.setIcon(R.mipmap.logo);
            builder.setTitle("未授权限" + retval);
            builder.setMessage("确认退出吗？");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
//								MainFragmentActivity.this.finish();
//                    finish();
                }
            });
            builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub

                }
            });
            builder.show();
        }
    }

    private void write(byte[] data){
        String s = toHexString(data,data.length);
        Log.i("MainActivity","串口写入数据："+s);
        if(LampApplication.driver!=null){
            int writeLength = LampApplication.driver.WriteData(data, data.length);//向串口写入数据，反回写入数据的长度
            if(writeLength<0){
                Toast.makeText(mContext,"写入失败，请重试！",Toast.LENGTH_SHORT).show();

            }else{
                Toast.makeText(mContext,"写入成功！",Toast.LENGTH_SHORT).show();
            }
        }

    }
    private class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[256];

            while (true) {
                if (!isOpen) {
                    break;
                }

                final int length = LampApplication.driver.ReadData(buffer, 256);
                if (length > 0) {
                    String data = toHexString(buffer, length);
                    Log.i("MainActivity","串口读到的信息:"+data);
                }

            }
        }
    }

    /**
     * 将byte[]数组转化为String类型
     *
     * @param arg    需要转换的byte[]数组
     * @param length 需要转换的数组长度
     * @return 转换后的String队形
     */
    public  String toHexString(byte[] arg, int length) {
        String result = "";
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }
}