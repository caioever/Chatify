package com.daniel0x7cc.chatify;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.daniel0x7cc.chatify.adapters.UsersAdapter;
import com.daniel0x7cc.chatify.helpers.GeoLocationManager;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.daniel0x7cc.chatify.models.User;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchPeopleFragment extends Fragment implements View.OnClickListener {

    private ImageView imGpsDisabled;
    private TextView tvEnableGps;
    private ImageView imGpsEnabled;
    private LinearLayout layoutSearchingUsers;
    private ListView listUsers;
    private long mLastClickTime = 0;
    private List<User> users;

    boolean isRequested = false;

    public SearchPeopleFragment() {
    }

    public static SearchPeopleFragment newInstance() {
        return new SearchPeopleFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_people, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEnableGps = (TextView) view.findViewById(R.id.fraSearchPeople_tvEnableGps);
        imGpsDisabled = (ImageView) view.findViewById(R.id.fraSearchPeople_imEnableGps);
        layoutSearchingUsers = (LinearLayout) view.findViewById(R.id.fraSearchPeople_viewSearching);
        imGpsEnabled = (ImageView) view.findViewById(R.id.fraSearchPeople_imGpsEnabled);
        listUsers = (ListView) view.findViewById(R.id.fraSearchPeople_lvUsers);
        users = new ArrayList<>();

        if(GeoLocationManager.getInstance().isGeoLocationEnabled()) {
            tvEnableGps.setVisibility(View.GONE);
            imGpsDisabled.setVisibility(View.GONE);
            getUsers();
        } else {
            tvEnableGps.setVisibility(View.VISIBLE);
            imGpsDisabled.setVisibility(View.VISIBLE);
            imGpsDisabled.setOnClickListener(this);
            tvEnableGps.setOnClickListener(this);
        }
    }

    private void openGPSSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, Consts.REQUEST_CODE_GPS_SETTINGS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Consts.REQUEST_CODE_GPS_SETTINGS) {
            if (GeoLocationManager.getInstance().isGeoLocationEnabled()) {
                tvEnableGps.setVisibility(View.GONE);
                imGpsDisabled.setVisibility(View.GONE);
                handleLocationSettingsChanged();
            }
        }
    }

    private void handleLocationSettingsChanged(){
        if (GeoLocationManager.getInstance().isGeoLocationEnabled()) {
            getUsers();
        }
    }

    private void getUsers(){
        layoutSearchingUsers.setVisibility(View.VISIBLE);
        final Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(1000);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        imGpsEnabled.startAnimation(animation);

        final UsersAdapter usersAdapter = new UsersAdapter(getContext());
        listUsers.setAdapter(usersAdapter);

        listUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                User user = usersAdapter.getItem(position);
                ChatActivity.startChat(getActivity(), user.getUserId(), user.getUserName(), "");
            }
        });

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();

        isRequested = true;
        final DatabaseReference usersReference = ref.child(Consts.USER_ACCOUNT_PATH);

                Query usersQuery = usersReference.orderByChild("userName");
                usersQuery.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            User opponentUser;
                            Location opponentLocation = new Location("");
                            users.clear();

                            if (PreferenceManager.getInstance().getLocation() != null) {

                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                opponentUser = ds.getValue(User.class);

                                if (opponentUser.getUserId() != null && !opponentUser.getUserId().equals(PreferenceManager.getInstance().getUserId())) {

                                    opponentLocation.setLongitude(opponentUser.getLongitude());
                                    opponentLocation.setLatitude(opponentUser.getLatitude());

                                    double distance = PreferenceManager.getInstance().getLocation().distanceTo(opponentLocation);
                                    int parsedDistance = (int) distance / 1000;

                                    if (parsedDistance <= Consts.DISTANCE_RADIUS && opponentUser.isShown()) {
                                        PreferenceManager.getInstance().saveDistance(opponentUser.getUserId(), parsedDistance);
                                        users.add(opponentUser);
                                    }
                                }
                            }

                                layoutSearchingUsers.setVisibility(View.GONE);
                                listUsers.setVisibility(View.VISIBLE);
                                usersAdapter.addAll(users);
                                usersAdapter.notifyDataSetChanged();

                        }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fraSearchPeople_imEnableGps:
                openGPSSettings();
                break;
            case R.id.fraSearchPeople_tvEnableGps:
                openGPSSettings();
                break;
        }
    }
}
