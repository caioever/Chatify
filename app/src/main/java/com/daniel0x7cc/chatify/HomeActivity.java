package com.daniel0x7cc.chatify;


import android.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.daniel0x7cc.chatify.customviews.CircleTransform;
import com.daniel0x7cc.chatify.helpers.FirebaseHelper;
import com.daniel0x7cc.chatify.helpers.GeoLocationManager;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.daniel0x7cc.chatify.helpers.SystemListeners;
import com.daniel0x7cc.chatify.interfaces.OnUpdateUnreadMessageCount;
import com.daniel0x7cc.chatify.services.MessageNotificationService;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.Picasso;

import me.leolin.shortcutbadger.ShortcutBadgeException;
import me.leolin.shortcutbadger.ShortcutBadger;

public class HomeActivity extends AppCompatActivity implements OnUpdateUnreadMessageCount {

    private Toolbar toolbar;
    FirebaseStorage mFirebaseStorage;
    private int countUnreadMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SendbirdHelper.getInstance().login();
        GeoLocationManager.getInstance().connect();

        // Start service to show chat notification with app on foreground
        Intent intent = new Intent(this, MessageNotificationService.class);
        startService(intent);

        mFirebaseStorage = FirebaseStorage.getInstance();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUserLocation();
        initUI();
    }

    private void setUserLocation(){
        if (Build.VERSION.SDK_INT >= 23 && !GeoLocationManager.getInstance().hasGeoLocationPermissions()) {
                    requestPermissions(new String[] {
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    Consts.REQUEST_CODE_GPS_PERMISSION);
        }

        if (GeoLocationManager.getInstance().hasGeoLocationPermissions()) {
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            DatabaseReference reference = firebaseDatabase.getReference().child(Consts.USER_ACCOUNT_PATH).child(FirebaseHelper.getInstance().getUserId());

            reference.child("latitude").setValue(GeoLocationManager.getInstance().getLatitude());
            reference.child("longitude").setValue(GeoLocationManager.getInstance().getLongitude());

            Location location = new Location("location");
            location.setLatitude(GeoLocationManager.getInstance().getLatitude());
            location.setLongitude(GeoLocationManager.getInstance().getLongitude());
            PreferenceManager.getInstance().setLocation(location);
        }
    }

    private void initUI(){

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        // Sets a bigger limit to the pager, inhibiting it from destroying a third view.
        pager.setOffscreenPageLimit(2);
        HomePageAdapter adapter = new HomePageAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        break;
                    case 1:

                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // ignored
            }
        });


        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(pager);

        setNavigationDrawer();
    }

    private void setNavigationDrawer(){
        new DrawerBuilder().withActivity(this).build();
        PrimaryDrawerItem itemProfile = new PrimaryDrawerItem().withIdentifier(1).withName(getString(R.string.my_profile)).withIcon(R.drawable.ic_person);
        SecondaryDrawerItem itemLogout = new SecondaryDrawerItem().withIdentifier(2).withName(getString(R.string.logout)).withIcon(R.drawable.ic_logout);

        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }
        });


        String avatar = PreferenceManager.getInstance().getAvatar();

        ProfileDrawerItem profileDrawerItem = new ProfileDrawerItem().withName(PreferenceManager.getInstance().getUsername())
                .withEmail(PreferenceManager.getInstance().getUserEmail());

        if (avatar != null && !avatar.isEmpty()){
            profileDrawerItem.withIcon(avatar);
        }

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.main_color)
                .addProfiles(profileDrawerItem)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

         new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        itemProfile,
                        new DividerDrawerItem(),
                        itemLogout, new SecondaryDrawerItem()
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        if (position == 1){
                            Intent intent = new Intent(HomeActivity.this, MyProfileActivity.class);
                            startActivity(intent);
                        }

                        if (position == 3){
                            App.getInstance().logout();
                            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        return true;
                    }
                })
                .build();
    }

    @Override
    protected void onDestroy() {
        // Stop service to show chat notification with app on foreground
        Intent intent = new Intent(this, MessageNotificationService.class);
        stopService(intent);

        // Disconnect SendBird
        SystemListeners.removeUpdateUnreadMessageCountListener(this);
        if (PreferenceManager.getInstance().isLogged() && !PreferenceManager.getInstance().hasChatOppened()) {
            SendbirdHelper.disconnect();
        }

        // Disconnect GPS
        GeoLocationManager.getInstance().disconnect();

        super.onDestroy();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Consts.REQUEST_CODE_GPS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    GeoLocationManager.getInstance().connect();
                    setUserLocation();
                    break;
                } else {
                    // Permission Denied
                    Toast.makeText(HomeActivity.this, getString(R.string.bad_request_gps), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onUpdateUnreadMessageCount(int count) {
        countUnreadMessages = count;
        updateShortcutBadger(countUnreadMessages);
        PreferenceManager.getInstance().setUnreadMessagesCount(countUnreadMessages);
    }

    private void updateShortcutBadger(int countNewUnreadMessages) {
        try {
            if (countNewUnreadMessages > 0) {
                ShortcutBadger.applyCountOrThrow(this, countNewUnreadMessages);
            } else {
                ShortcutBadger.removeCountOrThrow(this);
            }
        } catch (ShortcutBadgeException e) {
            LogUtils.e("Erro ao manipular badge do ícone do aplicativo.", e);
        }
    }
}
