package com.daniel0x7cc.chatify;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.daniel0x7cc.chatify.helpers.FirebaseHelper;
import com.daniel0x7cc.chatify.helpers.GeoLocationManager;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.models.User;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.ImageUtils;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


public class MyProfileActivity extends BaseActivity {

    FirebaseStorage mFirebaseStorage;
    private StorageReference mProfilePhotosReference;
    private ImageView imAvatar;
    private Uri currentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Meu Perfil");
        }

        ImageView imUploadAvatar = (ImageView) findViewById(R.id.actMyProfile_editAvatar);
         imAvatar = (ImageView) findViewById(R.id.actMyProfile_avatar);
            imUploadAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

        mFirebaseStorage = FirebaseStorage.getInstance();
        mProfilePhotosReference = mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH);

        if (PreferenceManager.getInstance().getAvatar() != null && !PreferenceManager.getInstance().getAvatar().isEmpty()) {
            Picasso.with(MyProfileActivity.this)
                    .load(PreferenceManager.getInstance().getAvatar())
                    .into(imAvatar);
        } else {
            mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH)
                    .child(PreferenceManager.getInstance().getUserId()).getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Log.e("photo", uri.toString());
                            Picasso.with(getBaseContext())
                                    .load(uri.toString())
                                    .fit()
                                    .error(R.drawable.placeholder_avatar)
                                    .placeholder(R.drawable.placeholder_avatar)
                                    .into(imAvatar);
                            PreferenceManager.getInstance().setAvatar(uri.toString());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                }
            });
        }

        TextView tvName = (TextView) findViewById(R.id.actMyProfile_name);
        tvName.setText(PreferenceManager.getInstance().getUsername());

        TextView tvEmail = (TextView) findViewById(R.id.actMyProfile_email);
        tvEmail.setText(PreferenceManager.getInstance().getUserEmail());

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();

        final DatabaseReference usersReference = ref.child(Consts.USER_ACCOUNT_PATH).child(FirebaseHelper.getInstance().getUserId());

        final Switch showMyself = (Switch) findViewById(R.id.actMyProfile_switch);
        showMyself.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMyself.setChecked(showMyself.isChecked());

                if (showMyself.isChecked()){
                    showVisibility(true);
                } else {
                    showVisibility(false);
                }
            }
        });

        Query usersQuery = usersReference.orderByChild("userName");
        usersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user.isShown()){
                    showMyself.setText("Visível para outros");
                } else {
                    showMyself.setText("Invisível para outros");
                }
                showMyself.setChecked(user.isShown());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        TextView tvLogout = (TextView) findViewById(R.id.actMyProfile_logout);
        tvLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.getInstance().logout();
                Intent intent = new Intent(MyProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    private void showVisibility(boolean flag){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference reference = firebaseDatabase.getReference().child(Consts.USER_ACCOUNT_PATH).child(FirebaseHelper.getInstance().getUserId());
        reference.child("shown").setValue(flag);
    }

    private void takePhoto(){
        new MaterialDialog.Builder(MyProfileActivity.this)
                .title(getString(R.string.pick_image))
                .items(R.array.upload_array_list)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which,
                                            CharSequence text) {
                        switch (which) {
                            case 0: // From camera
                                if (ActivityCompat.checkSelfPermission(MyProfileActivity.this,
                                        android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                                        || ActivityCompat.checkSelfPermission(MyProfileActivity.this,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(MyProfileActivity.this,
                                            new String[] { android.Manifest.permission.CAMERA,
                                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            Consts.REQUEST_CODE_PERMISSION_CAMERA_AVATAR);
                                } else {
                                    currentPhotoUri = App.getInstance().requestCaptureImageFromCamera(
                                            MyProfileActivity.this,
                                            Consts.REQUEST_CODE_AVATAR_FROM_CAMERA, "avatar");
                                }
                                break;
                            case 1: // From gallery
                                if(ActivityCompat.checkSelfPermission(MyProfileActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                                    ActivityCompat.requestPermissions(MyProfileActivity.this, new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                                    android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                            Consts.REQUEST_CODE_PERMISSION_GALLERY_AVATAR);
                                } else {
                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    intent.setType("image/*");
                                    startActivityForResult(intent, Consts.REQUEST_CODE_AVATAR_FROM_GALLERY);
                                }

                            default:
                                break;
                        }
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case Consts.REQUEST_CODE_AVATAR_FROM_CAMERA:
                if (currentPhotoUri != null) {
                    try {
                        LogUtils.e("selectedUri:" + currentPhotoUri.toString());
                        uploadPhoto(currentPhotoUri);
                    } catch (Exception e) {
                        LogUtils.e("Erro ao obter avatar da câmera.", e);
                        Toast.makeText(getBaseContext(), "Não foi possível carregar avatar da câmera.",
                                Toast.LENGTH_LONG).show();
                    }
                }
              //  Uri selectedImageUri = data.getData();

                //uploadPhoto(selectedImageUri);
                break;
        }
    }

    private void uploadPhoto(Uri photoUri){
                StorageReference photoRef = mProfilePhotosReference.child(FirebaseHelper.getInstance().getUserId());

                photoRef.putFile(photoUri).addOnSuccessListener(MyProfileActivity.this,
                        new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                Picasso.with(MyProfileActivity.this).load(downloadUrl).into(imAvatar);
                                /*
                                Tem que salvar a URL na tabela user
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                 */
                                PreferenceManager.getInstance().setAvatar(downloadUrl.toString());

                            }
                        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
