package com.chentianlong.ruichu;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by chentianlong on 2018/3/9.
 */

public class UploadActivity extends AppCompatActivity{
    
    private ImageView uploadView;
    private Bitmap bitmap;
    private Handler mHandler = new Handler();

    private ProgressDialog progressDialog;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_layout);
        showProgress();
        initView();
        //获取上传的图片的URL
        String url = getIntent().getStringExtra(MainActivity.IMAGE_URL);
        setPhoto(url);
    }



    private void initView() {
        uploadView = (ImageView)findViewById(R.id.upload_view);
    }

    private void setPhoto(final String url){
        //开启线程读取位图
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL bitmapUrl = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) bitmapUrl.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                            bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    //更新UI必须在主线程
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dismissProgress();
                            uploadView.setImageBitmap(bitmap);
                        }
                    });
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
    private void showProgress(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("正在加载图片");
        progressDialog.setMessage("请稍后....");
        progressDialog.show();
    }
    private void dismissProgress(){
        progressDialog.dismiss();
    }
}
