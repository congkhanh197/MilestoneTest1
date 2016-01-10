package com.hasbrain.milestonetest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.StringDef;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hasbrain.milestonetest.model.FacebookImage;
import com.hasbrain.milestonetest.model.FacebookPhotoResponse;
import com.hasbrain.milestonetest.model.converter.FacebookImageDeserializer;
import com.hasbrain.milestonetest.model.converter.FacebookPhotoResponseDeserializer;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TYPE_UPLOADED = "uploaded";
    public static final String TYPE_TAGGED = "tagged";
    public static final String PUBLISH_ACTIONS_PERMISSION = "publish_actions";
    private static final int REQUEST_IMAGE = 0x1;
    @Bind(R.id.rv_photos)
    RecyclerView rvPhotos;
    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;
    @Bind(R.id.fab)
    FloatingActionButton floatingActionButton;
    private Gson gson;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Your facebook photos");
        ButterKnife.bind(this);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(true);
                                        getUserPhotos(TYPE_UPLOADED, null);
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                }
        );
        rvPhotos.setLayoutManager(new LinearLayoutManager(this));
        final MarshMallowPermission marshMallowPermission = new MarshMallowPermission(this);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestingPermissions()){
                    marshMallowPermission.requestPermissionForCamera();
                }else{
                    openCameraForImage();
                }
            }
        });
        getUserPhotos(TYPE_UPLOADED, null);
        gson = new GsonBuilder()
                .registerTypeAdapter(FacebookImage.class, new FacebookImageDeserializer())
                .registerTypeAdapter(FacebookPhotoResponse.class, new FacebookPhotoResponseDeserializer())
                .create();
    }

    @Override
    public void onRefresh() {
        getUserPhotos(TYPE_UPLOADED, null);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*if (REQUEST_IMAGE == requestCode && resultCode == RESULT_OK) {
            Bitmap bitmapData = data.getParcelableExtra("data");
            if (bitmapData != null) {
                uploadPhotoToFacebook(bitmapData);
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }*/
        if (REQUEST_IMAGE == requestCode) {
            if (resultCode == RESULT_OK) {
                File file = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
                if (file.exists()){
                    InputStream is = null;
                    try {
                        is = new FileInputStream(file);
                        Bitmap bitmapData = BitmapFactory.decodeStream(is);
                        if (bitmapData != null) {
                            uploadPhotoToFacebook(bitmapData);
                        }
                        is.close();
                        is = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (is != null){
                            try {
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    file.delete();
                }
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

    }

    private boolean requestingPermissions() {
        MarshMallowPermission marshMallowPermission = new MarshMallowPermission(this);
        if (marshMallowPermission.checkPermissionForCamera()) {
            return false;
        } else {
            return true;
        }
    }

    private void openCameraForImage() {
            Intent openCameraForImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(openCameraForImageIntent, REQUEST_IMAGE);
    }

    private void uploadPhotoToFacebook(final Bitmap imageBitmap) {
        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        if (currentAccessToken.getPermissions().contains(PUBLISH_ACTIONS_PERMISSION)) {
            doUploadPhotoToFacebook(imageBitmap, currentAccessToken);
        } else {
            callbackManager = CallbackManager.Factory.create();
            LoginManager loginManager = LoginManager.getInstance();
            loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    if (loginResult.getRecentlyGrantedPermissions().contains(PUBLISH_ACTIONS_PERMISSION)) {
                        doUploadPhotoToFacebook(imageBitmap, loginResult.getAccessToken());
                    }
                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError(FacebookException error) {

                }
            });
            loginManager.logInWithPublishPermissions(this, Collections.singletonList(PUBLISH_ACTIONS_PERMISSION));

        }
    }

    private void doUploadPhotoToFacebook(Bitmap imageFile, AccessToken currentAccessToken) {
        GraphRequest graphRequest = GraphRequest
                .newUploadPhotoRequest(currentAccessToken, "me/photos", imageFile,
                        "Upload from hasBrain Milestone test", null, new GraphRequest.Callback() {
                            @Override
                            public void onCompleted(GraphResponse response) {
                                if (response.getError() != null) {
                                    Toast.makeText(MainActivity.this, "Image upload error " + response.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Upload image success", Toast.LENGTH_LONG).show();
                                    getUserPhotos(TYPE_UPLOADED, null);
                                }
                            }
                        });
        graphRequest.executeAsync();
    }

    private void getUserPhotos(@PHOTO_TYPE String photoType, final String after) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,images,picture,created_time,height,width");
        parameters.putString("type", photoType);
        if (after != null) {
            parameters.putString("after", after);
        }
        GraphRequest graphRequest = new GraphRequest(accessToken, "me/photos", parameters, HttpMethod.GET);
        graphRequest.setCallback(new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                Log.d("hasBrain", "Graph response " + response.toString());
                FacebookPhotoResponse facebookPhotoResponse = gson
                        .fromJson(response.getRawResponse(), FacebookPhotoResponse.class);
                displayPhotos(facebookPhotoResponse.getData());
            }
        });
        graphRequest.executeAsync();
    }

    private void displayPhotos(List<FacebookImage> data) {
        rvPhotos.setAdapter(new FacebookImageAdapter(getLayoutInflater(), Picasso.with(this), data));
    }

    @StringDef({TYPE_UPLOADED, TYPE_TAGGED})
    public @interface PHOTO_TYPE {

    }

    static class FacebookImageVH extends RecyclerView.ViewHolder {

        @Bind(R.id.iv_facebook_photo)
        ImageView ivFacebookPhoto;
        @Bind(R.id.tv_image_name)
        TextView tvImageName;
        @Bind(R.id.tv_image_time)
        TextView tvImageTime;
        @Bind(R.id.tv_size)
        TextView tvSize;
        private Picasso picasso;

        public FacebookImageVH(Picasso picasso, View itemView) {
            super(itemView);
            this.picasso = picasso;
            ButterKnife.bind(this, itemView);
        }

        public void bind(FacebookImage facebookImage) {
            picasso.load(facebookImage.getImageUrl()).into(ivFacebookPhoto);
            tvImageName.setText(facebookImage.getName());
            tvImageTime.setText(facebookImage.getCreatedTime());
            tvSize.setText(facebookImage.getHeight()+"x"+facebookImage.getWidth());
        }
    }

    private static class FacebookImageAdapter extends RecyclerView.Adapter<FacebookImageVH> {

        private LayoutInflater layoutInflater;
        private Picasso picasso;
        private List<FacebookImage> facebookImages;

        public FacebookImageAdapter(LayoutInflater layoutInflater, Picasso picasso,
                                    List<FacebookImage> facebookImages) {
            this.layoutInflater = layoutInflater;
            this.picasso = picasso;
            this.facebookImages = facebookImages;
        }

        @Override
        public FacebookImageVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = layoutInflater.inflate(R.layout.item_facebook_photo, parent, false);
            return new FacebookImageVH(picasso, itemView);
        }

        @Override
        public void onBindViewHolder(FacebookImageVH holder, int position) {
            holder.bind(facebookImages.get(position));
        }

        @Override
        public int getItemCount() {
            return facebookImages != null ? facebookImages.size() : 0;
        }
    }
}
