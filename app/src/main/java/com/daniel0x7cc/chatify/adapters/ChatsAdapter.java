package com.daniel0x7cc.chatify.adapters;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.daniel0x7cc.chatify.R;
import com.daniel0x7cc.chatify.helpers.FirebaseHelper;
import com.daniel0x7cc.chatify.helpers.SystemListeners;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.GlobalUtils;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.daniel0x7cc.chatify.utils.TimeUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ChatsAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater mInflater;
    private final ArrayList<GroupChannel> mItemList;
    private final ArrayList<User> mUsers;
    private String userId;
    private int unreadMessageCount;
    FirebaseStorage mFirebaseStorage;
    StorageReference mProfilePhotosReference;

    public ChatsAdapter(Context context) {
        this.context = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItemList = new ArrayList<GroupChannel>();
        mUsers = new ArrayList<User>();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mProfilePhotosReference = mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH);
    }

    @Override
    public int getCount() {
        return mItemList.size();
    }

    @Override
    public GroupChannel getItem(int position) {
        return mItemList.get(position);
    }

    public void clear() {
        mItemList.clear();
    }

    public GroupChannel remove(int index) {
        return mItemList.remove(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addAll(List<GroupChannel> channels) {
        if (channels != null && !channels.isEmpty()) {
            for (GroupChannel channel : channels) {
                if (channel.getMemberCount() > 1) {
                    mItemList.add(channel);
                }
            }
        }
    }

    public void replace(GroupChannel newChannel) {
        for(GroupChannel oldChannel : mItemList) {
            if(oldChannel.getUrl().equals(newChannel.getUrl())) {
                mItemList.remove(oldChannel);
                break;
            }
        }

        mItemList.add(0, newChannel);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ItemViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_chat, parent, false);
            holder = new ItemViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }

        GroupChannel item = getItem(position);

        if (userId == null && SendBird.getCurrentUser() != null) {
            userId = SendBird.getCurrentUser().getUserId();
        }

        User.ConnectionStatus connectionStatus;

        try {
            if (item.getMembers().get(1).getUserId().equals(userId)) {
                connectionStatus = item.getMembers().get(0).getConnectionStatus();
            } else {
                connectionStatus = item.getMembers().get(1).getConnectionStatus();
            }
        } catch (ArrayIndexOutOfBoundsException ex){
            ex.getMessage();
            connectionStatus = User.ConnectionStatus.NON_AVAILABLE;
        }

        if (connectionStatus == User.ConnectionStatus.ONLINE) {
            holder.imOnline.setVisibility(View.VISIBLE);
        } else {
            holder.imOnline.setVisibility(View.INVISIBLE);
        }

        if (item.getMembers().get(1).getUserId().equals(PreferenceManager.getInstance().getUserId())) {
            mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH)
                    .child(item.getMembers().get(0).getUserId()).getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
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

        } else {
            mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH)
                    .child(item.getMembers().get(1).getUserId()).getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
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
        }

        holder.tvName.setText(SendbirdHelper.getDisplayMemberNames(item.getMembers(), false));

        if(item.getUnreadMessageCount() > 0) {
            holder.tvUnreadMessages.setVisibility(View.VISIBLE);
            holder.tvUnreadMessages.setText(String.valueOf(item.getUnreadMessageCount()));
        } else {
            holder.tvUnreadMessages.setVisibility(View.GONE);
        }

        BaseMessage message = item.getLastMessage();

        if (message == null) {
            holder.tvLastMessageTime.setText(null);
        } else if (TimeUtils.getDisplayTimeOrDate(context, message.getCreatedAt()).equals(context.getString(R.string.chat_header_today))) {
            holder.tvLastMessageTime.setText(TimeUtils.getDisplayDateTime(context, message.getCreatedAt()));
        } else {
            holder.tvLastMessageTime.setText(TimeUtils.getDisplayTimeOrDate(context, message.getCreatedAt()));
        }

        if (item.isTyping()) {
            String userName = GlobalUtils.getFirstWord(SendbirdHelper.getOpponentNickname(item));
            String typingMessage = String.format(context.getString(R.string.is_chat_typing), userName);
            holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.main_color));
            holder.tvLastMessage.setText(typingMessage);

        } else if (message instanceof UserMessage) {
            holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.main_color));
            holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.color_gray));
            holder.tvLastMessage.setText(((UserMessage) message).getMessage());
            holder.imAudioMic.setVisibility(View.GONE);
        } else if (message instanceof AdminMessage) {
            holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.color_gray));
            holder.tvLastMessage.setText(((AdminMessage) message).getMessage());
            holder.imAudioMic.setVisibility(View.GONE);
        } else if (message instanceof FileMessage) {

            if (((FileMessage) message).getType().equals(Consts.KEY_AUDIO_MESSAGE)) {
                holder.tvLastMessage.setText(context.getString(R.string.str_audio_message) + " (" + ((FileMessage) message).getData() + ")");
                holder.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                holder.imAudioMic.setVisibility(View.VISIBLE);
                holder.imAudioMic.setImageResource(R.drawable.ic_microfone);
            } else {
                holder.imAudioMic.setVisibility(View.GONE);
                holder.tvLastMessage.setText("Foto");
            }
        }

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        unreadMessageCount = 0;
        if (mItemList != null && !mItemList.isEmpty()) {
            for (GroupChannel channel : mItemList) {
                unreadMessageCount += channel.getUnreadMessageCount();
            }
        }
        SystemListeners.notifyOnUpdateUnreadMessageCount(unreadMessageCount);
    }

    private static class ItemViewHolder {
        private ImageView imAvatar;
        private ImageView imOnline;
        private TextView tvName;
        private TextView tvLastMessage;
        private TextView tvLastMessageTime;
        private TextView tvUnreadMessages;
        private ImageView imAudioMic;

        private ItemViewHolder(View itemView) {
            imAvatar = (ImageView) itemView.findViewById(R.id.rowChat_imAvatar);
            imOnline = (ImageView) itemView.findViewById(R.id.rowChat_imOnline);
            tvName = (TextView) itemView.findViewById(R.id.rowChat_tvName);
            tvLastMessage = (TextView) itemView.findViewById(R.id.rowChat_tvLastMessage);
            tvLastMessageTime = (TextView) itemView.findViewById(R.id.rowChat_tvLastMessageTime);
            tvUnreadMessages = (TextView) itemView.findViewById(R.id.rowChat_tvUnreadMessages);
            imAudioMic = (ImageView) itemView.findViewById(R.id.rowChat_imAudio);
        }
    }
}
