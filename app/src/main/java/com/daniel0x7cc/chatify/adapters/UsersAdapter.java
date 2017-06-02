package com.daniel0x7cc.chatify.adapters;


import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.daniel0x7cc.chatify.HomeActivity;
import com.daniel0x7cc.chatify.R;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.models.User;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersAdapter extends BaseAdapter {

    private ArrayList<User> usersList;
    private Context context;
    private LayoutInflater inflater;
    FirebaseStorage mFirebaseStorage;
    StorageReference mProfilePhotosReference;

    public UsersAdapter(Context context){
        this.context = context;
        this.usersList = new ArrayList<>();
        this.inflater = ((HomeActivity) context).getLayoutInflater();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mProfilePhotosReference = mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH);
    }

    @Override
    public int getCount() {
        return usersList.size();
    }

    @Override
    public User getItem(int position) {
        return usersList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void add(User user){
        usersList.add(user);
    }

    public void clear(){
        usersList.clear();
    }

    public void addAll(List<User> users) {
        if (users != null && !users.isEmpty()) {
            usersList.clear();
            for (User user : users) {
                usersList.add(user);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ItemViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_user, parent, false);
            holder = new ItemViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }

        User user = getItem(position);

        holder.tvName.setText(user.getUserName());

        int parsedDistance = PreferenceManager.getInstance().getDistanceFromUser(user.getUserId());

        PreferenceManager.getInstance().setOpponentAvatar(user.getUserId(), user.getAvatarUrl());

            mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH)
                    .child(user.getUserId()).getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Log.e("photo", uri.toString());
                            Picasso.with(context)
                                    .load(uri.toString())
                                    .fit()
                                    .error(R.drawable.placeholder_avatar)
                                    .placeholder(R.drawable.placeholder_avatar)
                                    .into(holder.imAvatar);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                }
            });


        if (parsedDistance == 0){
            holder.tvDistance.setText(String.format(Locale.getDefault(), context.getString(R.string.near_distance_formatted), 1));
        } else {
            holder.tvDistance.setText(String.format(Locale.getDefault(), context.getString(R.string.distance_formatted), parsedDistance));
        }

        return convertView;
    }

    private static class ItemViewHolder {

        private TextView tvName;
        private TextView tvDistance;
        private ImageView imAvatar;

        private ItemViewHolder(View itemView) {
            tvName = (TextView) itemView.findViewById(R.id.rowUser_tvName);
            tvDistance = (TextView) itemView.findViewById(R.id.rowUser_tvDistance);
            imAvatar = (ImageView) itemView.findViewById(R.id.rowUser_imAvatar);
        }
    }
}
