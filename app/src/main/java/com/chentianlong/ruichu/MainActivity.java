package com.chentianlong.ruichu;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import com.qiniu.util.Auth;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    public static final int TAKE_PHOTO = 1;
    public static final String IMAGE_URL = "com.chentianlong.ruichu.image";//用Intent传递给UploadActivity时的key
    public static final String IMAGE_NAME = "iamge.jpg";//getExternalCacheDir()取得的图片名称
    public static final String DEFAULT_URL = "http://p5bdbk4yu.bkt.clouddn.com";//七牛云指定的域名

    private ImageView mImageView;//图片显示区域
    private Button takePhoto;//拍照按钮
    private Button upload;  //上传按钮

    private Uri imageUri;//图片Uri

    private static Handler mHander = new Handler();//在子线程中用于和主线程交互

    private boolean hasPicture = false;

    //在客户端中生成上传需要的token
    private String accessKey = "VqNIDKh9HpjVXOL6t-x0NyIwlVqB-6W6DNp2Yxnc";
    private String secretKey = "b8omyk_kGtTX4ul_Z5Nv2tHPkJCxAD6HhL3tQPDi";
    //七牛云创建的储存位置名字
    private String bucket = "images";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化界面
        initView();
        //绑定点击事件
        takePhoto.setOnClickListener(this);
        upload.setOnClickListener(this);
    }

    /**
     * 初始化控件
     */
    private void initView() {

        mImageView = (ImageView)findViewById(R.id.image_view);
        takePhoto = (Button)findViewById(R.id.take_photo);
        upload = (Button)findViewById(R.id.upload);
    }

    /**
     * 点击事件
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.take_photo:
                File image = new File(getExternalCacheDir(),IMAGE_NAME);
                try{
                    if(image.exists()){
                        image.delete();
                    }
                    image.createNewFile();
                }catch (Exception e){
                    e.printStackTrace();
                }
                //7.0以上版本需要使用封装过得Uri
                getUri(image);
                //6.0以上需要获取权限
                if(hasNoPermission(this,Manifest.permission.CAMERA)){
                    requestPermission(this,Manifest.permission.CAMERA,TAKE_PHOTO);
                }else{
                    takePhoto();
                }
                break;
            case R.id.upload:
                if(hasPicture){
                    final ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle("正在上传图片");
                    progressDialog.setMessage("上传中....");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    //开启子线程上传图片
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //系统现在秒数作为上传用的图片名称
                            String key = System.currentTimeMillis()+"";
                            //生成token
                            Auth auth = Auth.create(accessKey, secretKey);
                            String upToken = auth.uploadToken(bucket);

                            UploadManager uploadManager = new UploadManager();
                            String path = getExternalCacheDir()+"/"+IMAGE_NAME;//获取图片路径
                            File file = new File(path);
                            //上传图片
                            uploadManager.put(file, key, upToken, new UpCompletionHandler() {
                                @Override
                                public void complete(String key, ResponseInfo info, JSONObject response) {
                                    //成功则跳转至UploadActivity，并根据图片Url地址显示出图片
                                    if(info.isOK()){
                                        Log.d("upload","success");
                                        progressDialog.dismiss();
                                        Intent intent = new Intent(MainActivity.this,UploadActivity.class);
                                        intent.putExtra(IMAGE_URL,DEFAULT_URL+"/"+key);
                                        startActivity(intent);
                                    }else{
                                        //失败则提示
                                        progressDialog.dismiss();
                                        mHander.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if(hasPicture){
                                                    Toast.makeText(MainActivity.this,"未连接网络",Toast.LENGTH_SHORT).show();
                                                }else{
                                                    Toast.makeText(MainActivity.this,"没有图片可以上传",Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });

                                    }
                                }
                            },null);
                        }
                    }){

                    }.start();


                    break;
                }else{
                    Toast.makeText(this, "没有图片可以上传", Toast.LENGTH_SHORT).show();
                }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case TAKE_PHOTO:
                if(checkPermission(grantResults)){
                    takePhoto();
                }else{
                    Toast.makeText(this,"必须开启权限才能使用拍照功能！",Toast.LENGTH_SHORT).show();
                }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case TAKE_PHOTO:
                if(resultCode == RESULT_OK){
                    setPhoto();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 拍照
     */
    private void takePhoto(){
        Intent intent = new Intent ("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,TAKE_PHOTO);
    }

    /**
     * 设置图片
     */
    private void setPhoto(){
        try{
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            hasPicture = true;
            mImageView.setImageBitmap(bitmap);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    /**
     * 根据文件获取Uri
     * @param image
     */
    private void getUri(File image){
        if(Build.VERSION.SDK_INT>=24){

            imageUri = FileProvider.getUriForFile(
                    MainActivity.this,
                    "com.chentianlong.ruichu.fileprovider",
                    image);
        }else{
            imageUri = Uri.fromFile(image);
        }
    }

    /**
     * 判断是否通过权限
     * @param grantResults
     * @return
     */
    private boolean checkPermission(int[] grantResults){
        return grantResults.length>0&&grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 判断是否没有permissionType的权限
     * @param context
     * @param permissionType
     * @return
     */
    private boolean hasNoPermission(Context context,String permissionType){
        return ContextCompat.checkSelfPermission(context,permissionType)!=PackageManager.PERMISSION_GRANTED;
    }

    /**
     **请求获取permissionType的权限
     * @param activity
     * @param permissionType
     * @param code
     */
    private void requestPermission(Activity activity, String permissionType,int code){
        ActivityCompat.requestPermissions(activity,new String[]{permissionType},code);
    }

}
