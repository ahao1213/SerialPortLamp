package com.example.serialportlamp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.strictmode.NonSdkApiUsedViolation;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    public boolean isOpen;
    private int retval;
    private Context mContext;
    private ReadThread mReadThread;

    public final int baudRate = 6900;
    public byte baudRate_byte;
    public byte stopBit = 1;
    public byte dataBit = 8;
    public byte parity = 0;
    public byte flowControl = 0;
    private File mExcelFile;
    private BroadcastReceiver usbReceiver;

    private static final int VID = 6790;
    private static final int PID = 29987;
    private TextView mainReadStateTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mainReadStateTv = findViewById(R.id.main_read_state_tv);

        findViewById(R.id.main_init_seriaport_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                open();

//                UsbManager usb = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
//
//                HashMap<String, UsbDevice> deviceList = usb.getDeviceList();
////                Iterator<UsbDevice> iterator = deviceList.values().iterator();
//
//                Set<Map.Entry<String, UsbDevice>> entries = deviceList.entrySet();
//                Iterator<Map.Entry<String, UsbDevice>> iterator = entries.iterator();
//                while (iterator.hasNext()){
//                    Map.Entry<String, UsbDevice> next = iterator.next();
//                    Toast.makeText(mContext,"key:"+next.getKey()+"pid???"+next.getValue().getProductId()+"did:"+next.getValue().getDeviceId()+"vid:"+next.getValue().getVendorId(),Toast.LENGTH_LONG).show();
//                }

            }
        });

        findViewById(R.id.main_send_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] close = new byte[7];
                close[0] = (byte) 0xEB;
                close[1] = (byte) 0xF2;
                close[2] = (byte) 0x3F;
                close[3] = (byte) 0x00;
                close[4] = (byte) 0x85;
                close[5] = (byte) 0xC3;
                close[6] = (byte) 0xEB;
                write(close);
            }
        });
        findViewById(R.id.main_tow_tv).setOnClickListener(new View.OnClickListener() {
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

        findViewById(R.id.main_creat_excel_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*??????Excel*/
                creatFile("????????????");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(mExcelFile);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
        initCH34();

//        initUsbReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);//????????????

    }

    private void creatFile(String fileName) {
//        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && getAvailableStorage() > 1000000) {
//            Toast.makeText(mContext, "SD????????????", Toast.LENGTH_LONG).show();
//            return;
//        }
        File dir = new File(getFilePath());
        mExcelFile = new File(dir, fileName + ".xls");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String getFilePath() {
        String rootPath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rootPath = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        } else {
            rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return rootPath + "/ble";
    }

    /**
     * ??????SD????????????
     */
    private static long getAvailableStorage(Context context) {
        String root = context.getExternalFilesDir(null).getPath();
        StatFs statFs = new StatFs(root);
        long blockSize = statFs.getBlockSize();
        long availableBlocks = statFs.getAvailableBlocks();
        long availableSize = blockSize * availableBlocks;
        // Formatter.formatFileSize(context, availableSize);
        return availableSize;
    }


    public void initUsbReceiver() {
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                UsbDevice device = (UsbDevice) intent.getParcelableExtra("UsbManager.EXTRA_DEVICE");
                if (device != null) {
                    Toast.makeText(mContext, "pid???" + device.getDeviceId() + "----vid:" + device.getVendorId(), Toast.LENGTH_SHORT).show();
                }

                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    //??????
                    Toast.makeText(mContext, "???????????????", Toast.LENGTH_SHORT).show();
                } else {
                    //??????


                    Toast.makeText(mContext, "??????????????????", Toast.LENGTH_SHORT).show();
                    LampApplication.driver = new CH34xUARTDriver(
                            (UsbManager) getSystemService(Context.USB_SERVICE), MainActivity.this,
                            ACTION_USB_PERMISSION);
                    if (!LampApplication.driver.UsbFeatureSupported())// ????????????????????????USB HOST
                    {
                        Dialog dialog = new AlertDialog.Builder(mContext)
                                .setTitle("??????")
                                .setMessage("?????????????????????USB HOST?????????????????????????????????")
                                .setPositiveButton("??????",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface arg0,
                                                                int arg1) {
                                                finish();
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
//                        open();
                    }
                }
            }
        };
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, usbDeviceStateFilter);
    }

    private void initCH34() {
        LampApplication.driver = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), mContext,
                ACTION_USB_PERMISSION);

        if (!LampApplication.driver.UsbFeatureSupported())// ????????????????????????USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(this)
                    .setTitle("??????")
                    .setMessage("?????????????????????USB HOST?????????????????????????????????")
                    .setPositiveButton("??????",
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

        }
    }

    private void open() {
        if (!isOpen) {
            retval = LampApplication.driver.ResumeUsbList();
            Toast.makeText(mContext, "???????????????" + retval, Toast.LENGTH_SHORT).show();
            if (retval == -1)// ResumeUsbList??????????????????CH34X??????????????????????????????
            {
                Dialog dialog = new AlertDialog.Builder(mContext)
                        .setTitle("??????")
                        .setMessage("???????????????????????????????????????")
                        .setPositiveButton("??????",
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
            } else if (retval == 0) {
                mainReadStateTv.setText("?????????????????????");
                boolean b = LampApplication.driver.UartInit();
                toast("?????????????????????" + b);
                if (!b) {
                    Toast.makeText(mContext, "????????????????????????", Toast.LENGTH_SHORT).show();
                    return;
                }
                toast("??????????????????");
                isOpen = true;
                mReadThread = new ReadThread();
                mReadThread.start();//??????????????????????????????


                if (LampApplication.driver.SetConfig(baudRate, dataBit, stopBit, parity,//?????????????????????????????????????????????????????????
                        flowControl)) {
                    Toast.makeText(mContext, "??????????????????!",
                            Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(mContext, "??????????????????!",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//            builder.setIcon(R.mipmap.logo);
                builder.setTitle("????????????" + retval);
                builder.setMessage("??????????????????");
                builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
//								MainFragmentActivity.this.finish();
//                    finish();
                    }
                });
                builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub

                    }
                });
                builder.show();
            }
        }


    }

    private void toast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    private void write(byte[] data) {
        String s = toHexString(data, data.length);
        Toast.makeText(mContext, "?????????????????????" + s, Toast.LENGTH_SHORT).show();
        Log.i("MainActivity", "?????????????????????" + s);
        if (LampApplication.driver != null) {
            int writeLength = LampApplication.driver.WriteData(data, data.length);//???????????????????????????????????????????????????
            Toast.makeText(mContext, "??????????????????" + writeLength, Toast.LENGTH_SHORT).show();
            if (writeLength < 0) {
                Toast.makeText(mContext, "???????????????????????????", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(mContext, "???????????????", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[256];

            while (true) {
//                if (!isOpen) {
//                    break;
//                }

                final int length = LampApplication.driver.ReadData(buffer, 256);
//                Message msga = Message.obtain();
//                msga.obj = length + "";
//                handler.sendMessage(msga);
                if (length > 0) {
                    String data = toHexString(buffer, length);
                    Message msg = Message.obtain();
                    msg.obj = data + "";
                    handler.sendMessage(msg);


                    Log.i("MainActivity", "?????????????????????:" + data);
                }

            }
        }
    }

    int i = 0;
    Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            i++;
            mainReadStateTv.setText("??????????????????" + i + "???????????????:" + msg.obj);
            Toast.makeText(mContext, "????????????" + msg.obj, Toast.LENGTH_SHORT).show();
//				readText.append((String) msg.obj);
        }
    };


    /**
     * ???byte[]???????????????String??????
     *
     * @param arg    ???????????????byte[]??????
     * @param length ???????????????????????????
     * @return ????????????String??????
     */
    public String toHexString(byte[] arg, int length) {
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

    private void closeDevice() {
        Log.e("Rechar", "closeDevice: ");
        Toast.makeText(this, "closeDevice", Toast.LENGTH_LONG).show();
    }

    private void openDevice() {
        Log.e("Rechar", "openDevice: ");
        Toast.makeText(this, "openDevice", Toast.LENGTH_LONG).show();
    }


    private void openDeviceAndRequestDevice() {
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getVendorId() == VID && device.getProductId() == PID) {
                Intent intent = new Intent(ACTION_USB_PERMISSION);//?????????????????????
                PendingIntent pendingIntent =
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
                usbManager.requestPermission(device, pendingIntent);//????????????????????????????????????
            }
        }
    }

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))//??????????????????????????????
            {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))//??????????????????
                    {
                        if (device != null) {
                            openDevice();
                        }

                    } else {
                        Log.e("Rechar", "device not permission, device info:" + device.toString());
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                UsbDevice device = (UsbDevice)
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                assert device != null;
                if (VID == device.getVendorId() && PID == device.getProductId()) {
                    closeDevice();
                    openDeviceAndRequestDevice();
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                UsbDevice device = (UsbDevice)
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                assert device != null;
                if (VID == device.getVendorId() && PID == device.getProductId()) {
                    closeDevice();
                }
            }
        }
    };

}