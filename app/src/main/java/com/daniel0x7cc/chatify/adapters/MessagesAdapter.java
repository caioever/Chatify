package com.daniel0x7cc.chatify.adapters;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.daniel0x7cc.chatify.ChatActivity;
import com.daniel0x7cc.chatify.R;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.daniel0x7cc.chatify.interfaces.PlayAudioListener;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.GlobalUtils;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.daniel0x7cc.chatify.utils.TimeUtils;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class MessagesAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private static final int TYPE_UNSUPPORTED = 0;
    private static final int TYPE_ADMIN_MESSAGE = 1;
    private static final int TYPE_AUDIO_MESSAGE = 2;
    private static final int TYPE_FILE_MESSAGE = 3;
    private static final int TYPE_USER_MESSAGE = 4;
    private static final int TYPE_TYPING_INDICATOR = 5;

    private final ArrayList<Object> data = new ArrayList<>();
    private ArrayList<Boolean> selectedStates = new ArrayList<>();
    private SparseBooleanArray progressDownloadAudioShowed = new SparseBooleanArray();
    private final Context context;
    private final LayoutInflater inflater;
    private boolean playLastAudioMessage;
    private GroupChannel groupChannel;
    private String loggedUserId;
    private String opponentNickName;
    private PlayAudioListener playAudioListener;

    public MessagesAdapter(Activity activity, boolean playLastAudioMessage, PlayAudioListener playAudioListener) {
        this.context = activity.getApplicationContext();
        this.inflater = activity.getLayoutInflater();
        this.playLastAudioMessage = playLastAudioMessage;
        this.opponentNickName = context.getString(R.string.str_user);
        this.playAudioListener = playAudioListener;
    }

    @Override
    public int getViewTypeCount() {
        return 6;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= data.size()) {
            return TYPE_TYPING_INDICATOR;
        }

        Object item = data.get(position);

        if (item instanceof AdminMessage) {
            return TYPE_ADMIN_MESSAGE;
        }

        if (item instanceof FileMessage) {
            String fileType = ((FileMessage) item).getType();

            if (fileType != null && fileType.equals(Consts.KEY_AUDIO_MESSAGE)) {
                return TYPE_AUDIO_MESSAGE;
            } else {
                return TYPE_FILE_MESSAGE;
            }
        }

        if (item instanceof UserMessage) {
            return TYPE_USER_MESSAGE;
        }

        return TYPE_UNSUPPORTED;
    }

    @Override
    public int getCount() {
        if (groupChannel == null || !groupChannel.isTyping()) {
            return data.size();
        } else {
            return data.size() + 1;
        }
    }

    @Override
    public Object getItem(int position) {
        if (position >= data.size() && groupChannel != null) {
            List<User> members = groupChannel.getTypingMembers();
            ArrayList<String> names = new ArrayList<>();
            for (User member : members) {
                names.add(member.getNickname());
            }

            return names;
        }
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setGroupChannel(GroupChannel groupChannel) {
        this.groupChannel = groupChannel;
    }

    public void setOpponentNickName(String opponentNickName) {
        if (opponentNickName != null) {
            this.opponentNickName = GlobalUtils.getFirstWord(opponentNickName);
        }
    }

    public void delete(Object object) {
        int index = data.indexOf(object);
        if (index >= 0) {
            data.remove(index);
            selectedStates.remove(index);
        }
    }

    public void clear() {
        data.clear();
        selectedStates.clear();
    }

    public void insertMessage(BaseMessage message) {
        data.add(0, message);
        selectedStates.add(0, false);
    }

    public void appendMessage(BaseMessage message) {
        data.add(message);
        selectedStates.add(false);
    }

    public void setSelectedMessage(int position, boolean flag) {
        selectedStates.set(position, flag);
    }

    public void formatSelectedStates() {
        for (int i = 0; i < selectedStates.size(); i++) {
            selectedStates.set(i, false);
        }
    }

    @Override
    public long getHeaderId(int position) {
        final Object item = getItem(position);

        String timeString = null;
        if (item instanceof UserMessage) {
            UserMessage message = (UserMessage) item;
            timeString = TimeUtils.getDisplayTimeOrDate(context, message.getCreatedAt());

        } else if (item instanceof FileMessage) {
            FileMessage message = (FileMessage) item;
            timeString = TimeUtils.getDisplayTimeOrDate(context, message.getCreatedAt());

        } else if (item instanceof AdminMessage) {
            AdminMessage message = (AdminMessage) item;
            timeString = TimeUtils.getDisplayTimeOrDate(context, message.getCreatedAt());

        } else if (getItemViewType(position) == TYPE_TYPING_INDICATOR) {
            timeString = context.getString(R.string.chat_header_today);
        }

        if (timeString == null || timeString.isEmpty()) {
            return 0;

        } else if (timeString.equals(context.getString(R.string.chat_header_today))) {
            return 1;

        } else if (timeString.equals(context.getString(R.string.chat_header_yesterday))) {
            return 2;

        } else {
            timeString = timeString.replaceAll("[^0-9]", ""); // extrac only digits
            return Long.parseLong(timeString);
        }
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        String headerText;
        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_chat_header, parent, false);
            holder = new HeaderViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        final Object item = getItem(position);
        if (item instanceof BaseMessage) {
            BaseMessage message = (BaseMessage) item;
            headerText = TimeUtils.getDisplayTimeOrDate(context, message.getCreatedAt());
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(headerText.toUpperCase());
        }

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Object item = getItem(position);

        if (loggedUserId == null && SendBird.getCurrentUser() != null) {
            loggedUserId = SendBird.getCurrentUser().getUserId();
        }

        final int type = getItemViewType(position);
        switch (type) {
            case TYPE_ADMIN_MESSAGE:
                final AdminMessageViewHolder amHolder;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.row_admin_message, parent, false);
                    amHolder = new AdminMessageViewHolder(convertView);
                    convertView.setTag(amHolder);
                } else {
                    amHolder = (AdminMessageViewHolder) convertView.getTag();
                }
                AdminMessage adminMessage = (AdminMessage) item;
                fillAdminMessage(amHolder, adminMessage);
                break;

            case TYPE_FILE_MESSAGE:
                FileMessage fileMessage = (FileMessage) item;
                final FileMessageViewHolder fmHolder;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.row_file_message, parent, false);
                    fmHolder = new FileMessageViewHolder(convertView);
                    convertView.setTag(fmHolder);
                } else {
                    fmHolder = (FileMessageViewHolder) convertView.getTag();
                }
                fillFileMessage(fmHolder, fileMessage, position);
                break;

            case TYPE_AUDIO_MESSAGE:
                FileMessage audioMessage = (FileMessage) item;
                final AudioMessageViewHolder audioMessageHolder;
                final String audioDuration = ((FileMessage) item).getData();
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.row_audio_message, parent, false);
                    audioMessageHolder = new AudioMessageViewHolder(convertView);
                    convertView.setTag(audioMessageHolder);
                } else {
                    audioMessageHolder = (AudioMessageViewHolder) convertView.getTag();
                }
                fillAudioMessage(audioMessageHolder, audioMessage, audioDuration, position);
                break;

            case TYPE_USER_MESSAGE:
                UserMessage userMessage = (UserMessage) item;
                final UserMessageViewHolder umHolder;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.row_user_message, parent, false);
                    umHolder = new UserMessageViewHolder(convertView);
                    convertView.setTag(umHolder);
                } else {
                    umHolder = (UserMessageViewHolder) convertView.getTag();
                }
                fillUserMessage(umHolder, userMessage, position);
                break;

            case TYPE_TYPING_INDICATOR:
                final TypingIndicatorViewHolder tiHolder;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.row_typing_indicator, parent, false);
                    tiHolder = new TypingIndicatorViewHolder(convertView);
                    convertView.setTag(tiHolder);
                } else {
                    tiHolder = (TypingIndicatorViewHolder) convertView.getTag();
                }
                List list = (List) item;
                fillTypingIndicator(tiHolder, list);
                break;

            case TYPE_UNSUPPORTED:
            default:
                convertView = new View(inflater.getContext());
                return convertView;
        }

        return convertView;
    }

    private void fillAdminMessage(AdminMessageViewHolder holder, final AdminMessage message) {
        holder.tvMessage.setText(Html.fromHtml(message.getMessage()));
    }

    private void fillFileMessage(FileMessageViewHolder holder, final FileMessage message, int position) {
        if (message.getSender().getUserId().equals(loggedUserId)) {
            holder.containerLeft.setVisibility(View.GONE);
            holder.containerRight.setVisibility(View.VISIBLE);

            String url = message.getUrl();
            if (message.getThumbnails() != null && !message.getThumbnails().isEmpty()) {
                url = message.getThumbnails().get(0).getUrl();
            }

            if (url == null || url.isEmpty()) {
                holder.imRight.setImageResource(R.drawable.placeholder_image_error);
            } else {
                Picasso.with(context)
                        .load(url)
                        .placeholder(R.drawable.placeholder_loading_anim)
                        .error(R.drawable.placeholder_image_error)
                        .fit()
                        .into(holder.imRight);
            }

            holder.tvRightTime.setText(TimeUtils.getDisplayDateTime(context, message.getCreatedAt()));

            int unreadCount = groupChannel.getReadReceipt(message);
            if (unreadCount > 1) {
                holder.imRightStatus.setImageResource(0);
            } else if (unreadCount == 1) {
                holder.imRightStatus.setImageResource(R.drawable.ic_sent);
            } else {
                holder.imRightStatus.setImageResource(R.drawable.ic_sent_read);
            }

            if (selectedStates.get(position)) {
                holder.containerRightSelected.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_transparent));
            } else {
                holder.containerRightSelected.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            }

        } else {
            holder.containerLeft.setVisibility(View.VISIBLE);
            holder.containerRight.setVisibility(View.GONE);

            String url = message.getUrl();
            if (message.getThumbnails() != null && !message.getThumbnails().isEmpty()) {
                url = message.getThumbnails().get(0).getUrl();
            }

            if (url == null || url.isEmpty()) {
                holder.imLeft.setImageResource(R.drawable.placeholder_image_error);
            } else {
                Picasso.with(context)
                        .load(url)
                        .placeholder(R.drawable.placeholder_loading_anim)
                        .error(R.drawable.placeholder_image_error)
                        .fit()
                        .into(holder.imLeft);
            }

            holder.tvLeftTime.setText(TimeUtils.getDisplayDateTime(context, message.getCreatedAt()));

            if (selectedStates.get(position)) {
                holder.containerLeftSelected.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_transparent));
            } else {
                holder.containerLeftSelected.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            }
        }

    }

    private void fillUserMessage(UserMessageViewHolder holder, UserMessage message, int position) {
        if (position >= selectedStates.size()) {
            selectedStates.add(false);
        }

        if (message.getSender().getUserId().equals(loggedUserId)) {
            holder.containerLeft.setVisibility(View.GONE);
            holder.tvLeftMessage.setText(null);
            holder.tvLeftTime.setText(null);

            holder.containerRight.setVisibility(View.VISIBLE);
            holder.tvRightMessage.setText(message.getMessage());
            holder.tvRightTime.setText(TimeUtils.getDisplayDateTime(context, message.getCreatedAt()));

            int unreadCount = groupChannel.getReadReceipt(message);
            if (unreadCount > 1) {
                holder.imDeliveryStatus.setImageResource(0);
            } else if (unreadCount == 1) {
                holder.imDeliveryStatus.setImageResource(R.drawable.ic_sent);
            } else {
                holder.imDeliveryStatus.setImageResource(R.drawable.ic_sent_read);
            }

            if (selectedStates.get(position)) {
                holder.containerRightSelected.setVisibility(View.VISIBLE);
            } else {
                holder.containerRightSelected.setVisibility(View.GONE);
            }

        } else {
            holder.containerLeft.setVisibility(View.VISIBLE);
            holder.tvLeftMessage.setText(message.getMessage());
            holder.tvLeftTime.setText(TimeUtils.getDisplayDateTime(context, message.getCreatedAt()));

            holder.containerRight.setVisibility(View.GONE);
            holder.tvRightMessage.setText(null);
            holder.tvRightTime.setText(null);

            if (selectedStates.get(position)) {
                holder.containerLeftSelected.setVisibility(View.VISIBLE);
            } else {
                holder.containerLeftSelected.setVisibility(View.GONE);
            }
        }

            holder.containerBadge.setVisibility(View.GONE);

    }

    private void fillTypingIndicator(TypingIndicatorViewHolder holder, List list) {
        String userName = String.valueOf(list.get(0));
        String message = String.format(context.getString(R.string.is_chat_typing), userName);
        holder.tvMessage.setText(message);
    }

    private void fillAudioMessage(final AudioMessageViewHolder holder, final FileMessage message, String audioDuration, final int position) {
        String audioUrl;

        String path = context.getExternalFilesDir("Audio") + "/" + message.getName();
        LogUtils.e("filePath:" + path);
        File audioFile = new File(path);

        if (audioFile.exists()){
            audioUrl = audioFile.getAbsolutePath();
            progressDownloadAudioShowed.put(position, false);

            if (!progressDownloadAudioShowed.get(position)) {
                holder.downloadProgressRight.setVisibility(View.GONE);
                holder.imPlay.setVisibility(View.VISIBLE);
            } else {
                holder.downloadProgressRight.setVisibility(View.VISIBLE);
                holder.imPlay.setVisibility(View.GONE);
            }

        } else {
            audioUrl = "";
            if (message.getSender().getUserId().equals(loggedUserId)) {
                new AudioDownloadTask(holder, true, this, position).execute(message.getUrl(), message.getName());
                holder.tvAudioDuration.setText("");
            } else {
                new AudioDownloadTask(holder, false, this, position).execute(message.getUrl(), message.getName());
                holder.tvLeftAudioDuration.setText("");
            }
        }

        final MediaPlayer mediaPlayer = MediaPlayer.create(context, Uri.parse(audioUrl));

        if (message.getSender().getUserId().equals(loggedUserId)) {
            holder.leftContainer.setVisibility(View.GONE);
            holder.rightContainer.setVisibility(View.VISIBLE);
            holder.tvAudioDuration.setText(audioDuration);

            Picasso.with(context)
                    .load(PreferenceManager.getInstance().getAvatar())
                    .placeholder(R.drawable.placeholder_avatar)
                    .error(R.drawable.placeholder_avatar)
                    .fit()
                    .into(holder.imAvatar);

            holder.imPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mediaPlayer != null) {
                        playAudioListener.onPlayAudio(holder, mediaPlayer, new Timer(), false);
                    }
                }
            });

            holder.audioProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            holder.tvRightTime.setText(TimeUtils.getDisplayDateTime(context, message.getCreatedAt()));

            int unreadCount = groupChannel.getReadReceipt(message);
            if (unreadCount > 1) {
                holder.tvRightTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            } else if (unreadCount == 1) {
                holder.tvRightTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sent, 0);
            } else {
                holder.tvRightTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sent_read, 0);
            }

            if (selectedStates.get(position)) {
                holder.containerRightSelected.setVisibility(View.VISIBLE);
            } else {
                holder.containerRightSelected.setVisibility(View.GONE);
            }

        } else {

            if (!progressDownloadAudioShowed.get(position)) {
                holder.downloadProgressLeft.setVisibility(View.GONE);
                holder.imOpponentPlay.setVisibility(View.VISIBLE);
            } else {
                holder.downloadProgressLeft.setVisibility(View.VISIBLE);
                holder.imOpponentPlay.setVisibility(View.GONE);
            }

            holder.leftContainer.setVisibility(View.VISIBLE);
            holder.rightContainer.setVisibility(View.GONE);

            holder.tvLeftTime.setText(TimeUtils.getDisplayDateTime(context, message.getCreatedAt()));

            holder.tvLeftAudioDuration.setText(audioDuration);

            if (position == (getCount() - 1) && playLastAudioMessage) {
                playLastAudioMessage = false;
                playLastAudio(mediaPlayer, holder);
            }

            holder.imOpponentPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mediaPlayer != null) {
                        playAudioListener.onPlayAudio(holder, mediaPlayer, new Timer(), true);
                    }
                }
            });

            holder.audioOpponentBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if(fromUser && mediaPlayer != null){
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            String opponentAvatar = PreferenceManager.getInstance().getOpponentAvatar(SendbirdHelper.getOpponentId(groupChannel));

            if (opponentAvatar != null && !opponentAvatar.isEmpty()) {
                Picasso.with(context)
                        .load(opponentAvatar)
                        .placeholder(R.drawable.placeholder_avatar)
                        .error(R.drawable.placeholder_avatar)
                        .fit()
                        .into(holder.imOpponentAvatar);
            } else {
                Picasso.with(context)
                        .load(R.drawable.placeholder_avatar)
                        .placeholder(R.drawable.placeholder_avatar)
                        .error(R.drawable.placeholder_avatar)
                        .fit()
                        .into(holder.imOpponentAvatar);
            }

            if (selectedStates.get(position)) {
                holder.containerLeftSelected.setVisibility(View.VISIBLE);
            } else {
                holder.containerLeftSelected.setVisibility(View.GONE);
            }

        }
    }

    private void playLastAudio(final MediaPlayer mediaPlayerLeft, final AudioMessageViewHolder holder) {
        if (mediaPlayerLeft != null) {
            final Timer timer = new Timer();

            holder.audioOpponentBar.setMax(mediaPlayerLeft.getDuration());

            mediaPlayerLeft.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayerLeft.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    timer.cancel();
                    holder.imOpponentPlay.setImageResource(R.drawable.ic_play_audio);
                }
            });

            if (!mediaPlayerLeft.isPlaying()) {
                mediaPlayerLeft.start();
                holder.imOpponentPlay.setImageResource(R.drawable.ic_pause_audio);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (mediaPlayerLeft.isPlaying()) {
                            holder.audioOpponentBar.setProgress(mediaPlayerLeft.getCurrentPosition());
                        }
                    }
                }, 0, 100);

            } else {
                mediaPlayerLeft.pause();
                holder.imOpponentPlay.setImageResource(R.drawable.ic_play_audio);
                timer.cancel();
            }
        }
    }

    private class HeaderViewHolder {
        private TextView tvTime;

        private HeaderViewHolder(View itemView) {
            tvTime = (TextView) itemView.findViewById(R.id.rowChatHeader_tvTime);
        }
    }

    private static class AdminMessageViewHolder {
        private TextView tvMessage;

        private AdminMessageViewHolder(View itemView) {
            tvMessage = (TextView) itemView.findViewById(R.id.rowAdminMessage_tvMessage);
        }
    }

    private static class FileMessageViewHolder {
        private View containerLeft;
        private View containerLeftSelected;
        private ImageView imLeft;
        private TextView tvLeftTime;
        private View containerRight;
        private View containerRightSelected;
        private ImageView imRight;
        private ImageView imRightStatus;
        private TextView tvRightTime;

        private FileMessageViewHolder(View itemView) {
            containerLeft = itemView.findViewById(R.id.rowFileMessage_containerLeft);
            containerLeftSelected = itemView.findViewById(R.id.rowFileMessage_containerLeftSelected);
            imLeft = (ImageView) itemView.findViewById(R.id.rowFileMessage_imLeft);
            tvLeftTime = (TextView) itemView.findViewById(R.id.rowFileMessage_tvLeftTime);
            containerRight = itemView.findViewById(R.id.rowFileMessage_containerRight);
            containerRightSelected = itemView.findViewById(R.id.rowFileMessage_containerRightSelected);
            imRight = (ImageView) itemView.findViewById(R.id.rowFileMessage_imRight);
            imRightStatus = (ImageView) itemView.findViewById(R.id.rowFileMessage_imRightStatus);
            tvRightTime = (TextView) itemView.findViewById(R.id.rowFileMessage_tvRightTime);
        }
    }

    private static class UserMessageViewHolder {
        private FrameLayout containerBadge;
        private View containerLeft;
        private View containerLeftSelected;
        private TextView tvLeftMessage;
        private TextView tvLeftTime;
        private View containerRight;
        private View containerRightSelected;
        private TextView tvRightMessage;
        private TextView tvRightTime;
        private ImageView imDeliveryStatus;

        private UserMessageViewHolder(View itemView) {
            containerBadge = (FrameLayout) itemView.findViewById(R.id.rowUserMessage_containerBadge);
            containerLeft = itemView.findViewById(R.id.rowUserMessage_containerLeft);
            containerLeftSelected = itemView.findViewById(R.id.rowUserMessage_containerLeftSelected);
            tvLeftMessage = (TextView) itemView.findViewById(R.id.rowUserMessage_tvLeftMessage);
            tvLeftTime = (TextView) itemView.findViewById(R.id.rowUserMessage_tvLeftTime);
            containerRight = itemView.findViewById(R.id.rowUserMessage_containerRight);
            containerRightSelected = itemView.findViewById(R.id.rowUserMessage_containerRightSelected);
            tvRightMessage = (TextView) itemView.findViewById(R.id.rowUserMessage_tvRightMessage);
            tvRightTime = (TextView) itemView.findViewById(R.id.rowUserMessage_tvRightTime);
            imDeliveryStatus = (ImageView) itemView.findViewById(R.id.rowUserMessage_imDeliveryStatus);
        }
    }

    private static class TypingIndicatorViewHolder {
        private TextView tvMessage;

        private TypingIndicatorViewHolder(View itemView) {
            tvMessage = (TextView) itemView.findViewById(R.id.rowTypingIndicator_tvMessage);
        }
    }

    public static class AudioMessageViewHolder {

        public TextView tvRightTime;
        public TextView tvLeftTime;
        public ImageView imAvatar;
        public ImageView imOpponentAvatar;
        public ImageView imPlay;
        public ImageView imOpponentPlay;
        public View rightContainer;
        public View leftContainer;
        public SeekBar audioProgressBar;
        public SeekBar audioOpponentBar;
        public TextView tvAudioDuration;
        public TextView tvLeftAudioDuration;
        public ProgressBar downloadProgressRight;
        public ProgressBar downloadProgressLeft;

        private View containerLeftSelected;
        private View containerRightSelected;

        private AudioMessageViewHolder(View itemView) {
            tvRightTime = (TextView) itemView.findViewById(R.id.rowAudio_tvRightTime);
            tvAudioDuration = (TextView) itemView.findViewById(R.id.rowAudioRight_tvAudioDuration);
            imAvatar = (ImageView) itemView.findViewById(R.id.rowAudio_imAvatar);
            imPlay = (ImageView) itemView.findViewById(R.id.rowAudio_imPlay);
            rightContainer = itemView.findViewById(R.id.rowAudioMessage_containerRightSelectable);
            leftContainer = itemView.findViewById(R.id.rowAudioMessage_containerLeft);
            audioProgressBar = (SeekBar) itemView.findViewById(R.id.rowAudioMessage_audioProgressBar);
            containerRightSelected = itemView.findViewById(R.id.rowAudio_containerRightSelected);
            downloadProgressRight = (ProgressBar) itemView.findViewById(R.id.rowAudioRight_progressUpload);
            downloadProgressLeft = (ProgressBar) itemView.findViewById(R.id.rowAudioLeft_progressUpload);

            containerLeftSelected = itemView.findViewById(R.id.rowAudio_containerLeftSelected);
            tvLeftTime = (TextView) itemView.findViewById(R.id.rowAudioLeft_tvTime);
            tvLeftAudioDuration = (TextView) itemView.findViewById(R.id.rowAudioLeft_tvAudioDuration);
            imOpponentAvatar = (ImageView) itemView.findViewById(R.id.rowAudioLeft_imAvatar);
            imOpponentPlay = (ImageView) itemView.findViewById(R.id.rowAudioLeft_imPlay);
            audioOpponentBar = (SeekBar) itemView.findViewById(R.id.rowAudioLeft_audioProgressBar);
        }
    }

    private class AudioDownloadTask extends AsyncTask<String, String, String> {
        private MessagesAdapter adapter;
        private AudioMessageViewHolder holder;
        private boolean isRightMessage;
        private int position;

        private AudioDownloadTask(AudioMessageViewHolder audioMessageViewHolder,
                                  boolean isRightMessage, MessagesAdapter adapter, int position) {
            this.adapter = adapter;
            this.holder = audioMessageViewHolder;
            this.isRightMessage = isRightMessage;
            this.position = position;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDownloadAudioShowed.put(position, true);

            if (progressDownloadAudioShowed.get(position)) {
                if (isRightMessage) {
                    holder.imPlay.setVisibility(View.GONE);
                    holder.downloadProgressRight.setVisibility(View.VISIBLE);
                } else {
                    holder.imOpponentPlay.setVisibility(View.GONE);
                    holder.downloadProgressLeft.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream in = null;
            OutputStream ou = null;

            try {
                URL url = new URL(params[0]);
                String fileName = params[1];

                URLConnection conection = url.openConnection();
                conection.connect();

                // input stream to read file - with 8k buffer
                in = new BufferedInputStream(url.openStream(), 8192);

                LogUtils.e("fileName: " + fileName);
                // Output stream to write file
                File fileAudio = new File(context.getExternalFilesDir("Audio"), fileName);
                ou = new FileOutputStream(fileAudio);
                byte data[] = new byte[1024];

                int count;
                while ((count = in.read(data)) != -1) {
                    // writing data to file
                    ou.write(data, 0, count);
                }

                // flushing output
                ou.flush();
            } catch (Exception e) {
                LogUtils.e("Erro ao baixar mensagem de audio.", e);

            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {}
                }
                if (ou != null) {
                    try {
                        ou.close();
                    } catch (IOException ignored) {}
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String fileUrl) {
            LogUtils.e("final file url: " + fileUrl);
            this.adapter.notifyDataSetChanged();
        }
    }
}