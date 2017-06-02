package com.daniel0x7cc.chatify;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.BaseActivity;
import com.daniel0x7cc.chatify.R;
import com.daniel0x7cc.chatify.adapters.MessagesAdapter;
import com.daniel0x7cc.chatify.customviews.ViewProxy;
import com.daniel0x7cc.chatify.helpers.PreferenceManager;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.daniel0x7cc.chatify.interfaces.PlayAudioListener;
import com.daniel0x7cc.chatify.interfaces.SendBirdEventListener;
import com.daniel0x7cc.chatify.models.SelectedMessage;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.FileUtils;
import com.daniel0x7cc.chatify.utils.ImageUtils;
import com.daniel0x7cc.chatify.utils.KeyboardUtils;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.daniel0x7cc.chatify.utils.NetworkUtils;
import com.daniel0x7cc.chatify.utils.TimeUtils;
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
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.PreviousMessageListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;
import com.sendbird.android.shadow.com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ChatActivity extends BaseActivity implements SendBirdEventListener, PlayAudioListener {

    public static final String HASH_BADGE_OLD = "#(087671b0a84fdbd6418fe897b41946e0)";
    public static final String CUSTOM_TYPE_BADGE = "BADGE";
    private static final String identifier = "SendBirdGroupChat";

    private final LongSparseArray<BaseMessage> messagesCopied = new LongSparseArray<>();
    private ArrayList<SelectedMessage> forwardedMessages;
    private View viewDefault;
    private TextView tvCountUnreadChats;
    private TextView tvUserName;
    private ImageView imAvatar;
    private EditText etMessage;
    private View viewMsgSelected;
    private TextView tvCountMsgCopied;
    private ImageView imDelete;
    private ImageView imCopy;
    private StickyListHeadersListView listView;
    private MessagesAdapter adapter;
    private ImageView imUpload;
    private ProgressBar progressUpload;
    private ProgressBar progressLoading;
    private String channelUrl;
    private GroupChannel mGroupChannel;
    private PreviousMessageListQuery mPrevMessageListQuery;
    private Uri currentPhotoUri;
    private int countUnreadMessages;
    private String opponentId;
    private String opponentName;
    private boolean sendingBadge;
    private boolean mIsUploading;
    private String lastScreen;
    private View customView;
    private TextView tvLastSeen;

    // Audio feature
    private float startedDraggingX = -1;
    private float distCanMove = dp(80);
    private long startTime = 0L;
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    private Timer recordAudioTimer;
    private TextView recordTimeText;
    private ImageView imAudioRec;
    private Animation animation;

    private ImageButton imSendAudio;
    private ImageButton imSendMessage;

    private View messageView;
    private View audioView;
    private float dX;
    private String fileName;
    private ViewPropertyAnimator viewPropertyAnimator;

    private MediaRecorder mediaRecorder;

    public static void startChat(final Context context, final String opponentId,
                                 final String opponentName, final String opponentAvatarUrl) {
        if (!NetworkUtils.isOnline(context)) {
            Toast.makeText(context, context.getString(R.string.sb_send_message), Toast.LENGTH_SHORT).show();
            return;
        }

       // SendbirdHelper.getInstance().setAvatar(opponentId, opponentAvatarUrl);

        String[] userIds = new String[] { String.valueOf(opponentId) };

        GroupChannel.createChannelWithUserIds(Arrays.asList(userIds), true, "Channel Name", null,
                null, new GroupChannel.GroupChannelCreateHandler() {
                    @Override
                    public void onResult(final GroupChannel groupChannel, SendBirdException e) {
                        if (e != null) {
                            LogUtils.e("Erro ao iniciar chat via método startChat. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                            Toast.makeText(context, "Não foi possível iniciar conversa.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(Consts.KEY_CHAT_CHANNEL_URL, groupChannel.getUrl());
                        intent.putExtra(Consts.KEY_CHAT_OPONNENT_ID, opponentId);
                        intent.putExtra(Consts.KEY_CHAT_OPONNENT_NAME, opponentName);
                        intent.putExtra(Consts.KEY_CHAT_OPENED_BY_NOTIFICATION, false);
                        context.startActivity(intent);
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sendingBadge = false;

        Intent intent = getIntent();

        boolean hideKeyboard = intent.getBooleanExtra(Consts.KEY_CHAT_HIDE_KEYBOARD, false);

        if (hideKeyboard) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        //audioButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.button_hit);
        opponentName = intent.getStringExtra(Consts.KEY_CHAT_OPONNENT_NAME);
        countUnreadMessages = intent.getIntExtra(Consts.KEY_CHAT_UNREAD_MESSAGES, 0);
        channelUrl = intent.getStringExtra(Consts.KEY_CHAT_CHANNEL_URL);
        opponentId = intent.getStringExtra(Consts.KEY_CHAT_OPONNENT_ID);
        forwardedMessages = (ArrayList<SelectedMessage>) intent.getSerializableExtra(Consts.KEY_CHAT_FORWARDED_MSGS);

        animation = new AlphaAnimation(1, 0);
        animation.setDuration(1000);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);

        boolean openedByNotification = intent.getBooleanExtra(Consts.KEY_CHAT_OPENED_BY_NOTIFICATION, false);
       // boolean playLastAudioMessage = intent.getBooleanExtra(Consts.KEY_PLAY_LAST_AUDIO_MESSAGE, false);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setCustomView(R.layout.actionbar_chat);

            customView = actionBar.getCustomView();

            viewDefault = customView.findViewById(R.id.actionBar_viewDefault);
            viewDefault.setVisibility(View.VISIBLE);

            viewMsgSelected = customView.findViewById(R.id.actionBar_viewMsgSelected);
            viewMsgSelected.setVisibility(View.GONE);

            tvCountMsgCopied = (TextView) customView.findViewById(R.id.actionBar_tvCountMsgCopied);
            imDelete = (ImageView) customView.findViewById(R.id.actionBar_imDelete);
            imDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (messagesCopied.size() <= 0) {
                        restoreActionBar();
                        return;
                    }

                    final String userId = String.valueOf(PreferenceManager.getInstance().getUserId());

                    for (int i = 0; i < messagesCopied.size(); i++) {
                        long key = messagesCopied.keyAt(i);
                        final BaseMessage message = messagesCopied.get(key);

                        String senderId = null;
                        if (message instanceof UserMessage) {
                            UserMessage userMessage = (UserMessage) message;
                            senderId = userMessage.getSender().getUserId();

                        } else if (message instanceof FileMessage) {
                            FileMessage fileMessage = (FileMessage) message;
                            senderId = fileMessage.getSender().getUserId();
                        }

                        if (senderId == null || !senderId.equals(userId)) {
                            return;
                        }

                        mGroupChannel.deleteMessage(message, new BaseChannel.DeleteMessageHandler() {
                            @Override
                            public void onResult(SendBirdException e) {
                                if (e != null) {
                                    LogUtils.e("Erro ao excluir mensagem do chat. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                                    Toast.makeText(ChatActivity.this, "Não foi possível excluir mensagem. Por favor, tente novamente.", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                adapter.delete(message);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }

                    Toast.makeText(ChatActivity.this, getString(R.string.chat_msg_deleted), Toast.LENGTH_LONG).show();
                    restoreActionBar();
                }
            });

            imCopy = (ImageView) customView.findViewById(R.id.actionBar_imCopy);
            imCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    copyMessageToClipboard(getCopiedMessages());
                    restoreActionBar();
                }
            });

            final ImageView imForward = (ImageView) customView.findViewById(R.id.actionBar_imForward);
            imForward.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!NetworkUtils.isOnline(ChatActivity.this)) {
                        Toast.makeText(ChatActivity.this, getString(R.string.sb_send_message), Toast.LENGTH_SHORT).show();
                        return;
                    }

//                    Intent intent = new Intent(ChatActivity.this, SelectContactChatActivity.class);
//                    intent.putExtra(Consts.KEY_CHAT_FORWARDED_MSGS, getCopiedMessages());
//                    startActivity(intent);
                }
            });

            imAvatar = (ImageView) customView.findViewById(R.id.actionBar_imAvatar);
            imAvatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                 //   Intent intent = new Intent(ChatActivity.this, PartnerProfileActivity.class);
                  //  startActivity(intent);
                }
            });

            loadOpponentAvatar(opponentId);

            ImageView imBack = (ImageView) customView.findViewById(R.id.actionBar_imBack);
            imBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });

            tvCountUnreadChats = (TextView) customView.findViewById(R.id.actionBar_tvCountUnreadChats);
            tvCountUnreadChats.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });

            tvUserName = (TextView) customView.findViewById(R.id.actionBar_tvUserName);
            tvUserName.setText(opponentName);



        }

        listView = (StickyListHeadersListView) findViewById(R.id.actChat_listView);
        adapter = new MessagesAdapter(ChatActivity.this, false, this);
        listView.setAdapter(adapter);

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                KeyboardUtils.hideKeyboard(ChatActivity.this);
                return false;
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (view.getFirstVisiblePosition() == 0 && view.getChildCount() > 0 && view.getChildAt(0).getTop() == 0) {
                        loadPrevMessages(false);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Object item = listView.getItemAtPosition(position);
                if (item instanceof FileMessage) {
                    FileMessage message = (FileMessage) item;
                    if(!message.getType().equals(Consts.KEY_AUDIO_MESSAGE)) {
                        Intent intent = new Intent(ChatActivity.this, ImageViewerActivity.class);
                        intent.putExtra(Consts.KEY_USER_NAME, message.getSender().getNickname());
                        intent.putExtra(Consts.KEY_URL, message.getUrl());
                        startActivity(intent);
                    }
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                boolean consumedLongClick = false;

                final Object item = listView.getItemAtPosition(position);

                if (item instanceof UserMessage) {
                    UserMessage message = (UserMessage) item;
                    if (!message.getMessage().contains(HASH_BADGE_OLD)) {
                        if (messagesCopied.indexOfKey(message.getMessageId()) >= 0) {
                            adapter.setSelectedMessage(position, false);
                            messagesCopied.remove(message.getMessageId());
                        } else {
                            messagesCopied.put(message.getMessageId(), message);
                            adapter.setSelectedMessage(position, true);
                        }
                        adapter.notifyDataSetChanged();
                        consumedLongClick = true;
                    }

                } else if (item instanceof FileMessage) {
                    FileMessage message = (FileMessage) item;
                    if (messagesCopied.indexOfKey(message.getMessageId()) >= 0) {
                        adapter.setSelectedMessage(position, false);
                        messagesCopied.remove(message.getMessageId());
                    } else {
                        imCopy.setVisibility(View.GONE);
                        messagesCopied.put(message.getMessageId(), message);
                        adapter.setSelectedMessage(position, true);
                    }
                    adapter.notifyDataSetChanged();
                    consumedLongClick = true;
                }

                if (item instanceof BaseMessage) {
                    BaseMessage message = (BaseMessage) item;
                    if (messagesCopied.size() == 0) {
                        restoreActionBar();
                    } else {
                        setUpActionBar(messagesCopied.size(), message);
                    }
                }

                return consumedLongClick;
            }
        });

        progressUpload = (ProgressBar) findViewById(R.id.actChat_progressUpload);
        progressUpload.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, R.color.main_color), PorterDuff.Mode.SRC_IN);

        imUpload = (ImageView) findViewById(R.id.actChat_imUpload);
        imUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectImagePopup();
            }
        });

        imSendAudio = (ImageButton) findViewById(R.id.actChat_imAudio);
        imSendMessage = (ImageButton) findViewById(R.id.actChat_imMessage);
        imAudioRec = (ImageView) findViewById(R.id.actChat_imAudioRec);
        //imSend.setEnabled(false);
        viewPropertyAnimator = imSendAudio.animate();

        etMessage = (EditText) findViewById(R.id.actChat_etMessage);
       // etMessage.requestFocus();
        etMessage.setSelection(etMessage.getText().toString().length());
        etMessage.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (etMessage.getText().length() > 0) {
                    imSendAudio.setVisibility(View.GONE);
                    imSendMessage.setVisibility(View.VISIBLE);
                } else {
                    imSendAudio.setVisibility(View.VISIBLE);
                    imSendMessage.setVisibility(View.GONE);
                    imSendAudio.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mGroupChannel != null) {
                    if (editable.length() == 1) {
                        mGroupChannel.startTyping();
                    } else if (editable.length() <= 0) {
                        mGroupChannel.endTyping();
                    }
                }
            }
        });

        final TextView tvSlideToCancel = (TextView) findViewById(R.id.actChat_tvSlideToCancel);
        recordTimeText = (TextView) findViewById(R.id.actChat_recordingTimeText);

        messageView  = findViewById(R.id.actChat_viewMessage);
        audioView = findViewById(R.id.actChat_audio);


        imSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserMessage(etMessage.getText().toString());
                etMessage.setText(null);
                imSendMessage.setVisibility(View.GONE);
                imSendAudio.setVisibility(View.VISIBLE);
            }
        });

        imSendAudio.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (ActivityCompat.checkSelfPermission(ChatActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(ChatActivity.this,
                        android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ChatActivity.this,
                            new String[] {Manifest.permission.RECORD_AUDIO,
                                    android.Manifest.permission.RECORD_AUDIO},
                            Consts.REQUEST_CODE_PERMISSION_AUDIO);
                } else {

                    switch (motionEvent.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            messageView.setVisibility(View.GONE);
                            audioView.setVisibility(View.VISIBLE);

                            LinearLayout.LayoutParams startingParams = (LinearLayout.LayoutParams) tvSlideToCancel.getLayoutParams();

                            startingParams.leftMargin = dp(30);
                            tvSlideToCancel.setLayoutParams(startingParams);
                            ViewProxy.setAlpha(tvSlideToCancel, 1f);
                            startedDraggingX = -1;
                            dX = view.getX() - motionEvent.getRawX();

                            ViewCompat.animate(view).scaleX(2f).scaleY(2f).setDuration(1).start();
                            startRecord();

                            imSendAudio.getParent().requestDisallowInterceptTouchEvent(true);
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:

                            if (tvSlideToCancel.getAlpha() >= 0.01f){
                                startedDraggingX = -1;
                                stopRecord(true);
                                break;
                            }

                        case MotionEvent.ACTION_MOVE:

                            float x = motionEvent.getX();

                            if (tvSlideToCancel.getAlpha() < 0.01f) {
                                viewPropertyAnimator
                                        .x(0) // return to initial location
                                        .start();
                                stopRecord(false);
                                break;
                            } else {
                                viewPropertyAnimator
                                        .x(motionEvent.getRawX() + dX)
                                        .setDuration(0)
                                        .start();
                            }

                            x = x + ViewProxy.getX(view);

                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tvSlideToCancel.getLayoutParams();
                            if (startedDraggingX != -1) {
                                float dist = (x - startedDraggingX);
                                params.leftMargin = dp(30) + (int) dist;
                                tvSlideToCancel.setLayoutParams(params);
                                float alpha = 1.0f + dist / distCanMove;
                                if (alpha > 1) {
                                    alpha = 1f;
                                } else if (alpha < 0) {
                                    alpha = 0f;
                                }
                                ViewProxy.setAlpha(tvSlideToCancel, alpha);
                            }
                            if (x <= ViewProxy.getX(tvSlideToCancel) + tvSlideToCancel.getWidth()
                                    + dp(480)) {
                                if (startedDraggingX == -1) {
                                    startedDraggingX = x;
                                    distCanMove = (audioView.getMeasuredWidth()
                                            - tvSlideToCancel.getMeasuredWidth() - dp(48)) / 2.0f;
                                    if (distCanMove <= 0) {
                                        distCanMove = dp(480);
                                    } else if (distCanMove > dp(480)) {
                                        distCanMove = dp(480);
                                    }
                                }
                            }
                            if (params.leftMargin > dp(480)) {
                                params.leftMargin = dp(30);
                                tvSlideToCancel.setLayoutParams(params);
                                ViewProxy.setAlpha(tvSlideToCancel, 1f);
                                startedDraggingX = -1;
                            }

                            break;
                    }
                }
                view.onTouchEvent(motionEvent);
                return false;
            }
        });

        progressLoading = (ProgressBar) findViewById(R.id.actChat_progressLoading);
        progressLoading.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.main_color   ), PorterDuff.Mode.SRC_IN);
        showProgress();

        if (openedByNotification) {
            SendbirdHelper.addConnectListener(this);
            SendbirdHelper.init(this);
            SendbirdHelper.getInstance().login();
        } else {
            initGroupChannel();
        }
    }

    @Override
    protected void onDestroy() {
        SendbirdHelper.removeConnectListener(this);
        mediaRecorder = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsUploading) {
            SendBird.addChannelHandler(identifier, new SendBird.ChannelHandler() {
                @Override
                public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                    if (baseChannel.getUrl().equals(channelUrl)) {
                        mGroupChannel.markAsRead();
                        if (adapter != null) {
                            adapter.appendMessage(baseMessage);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        setTotalUnreadMessages(++countUnreadMessages);
                    }
                }

                @Override
                public void onReadReceiptUpdated(GroupChannel channel) {
                    if (channel.getUrl().equals(channelUrl)) {
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onTypingStatusUpdated(GroupChannel groupChannel) {
                    if (groupChannel.getUrl().equals(channelUrl)) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
            refreshGroupChannel();

        } else {
            mIsUploading = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mIsUploading) {
            SendBird.removeChannelHandler(identifier);
        }
    }

    private void startRecord() {
        startTime = SystemClock.uptimeMillis();
        recordTimeText.setText(getString(R.string.zero_time));

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        if (getExternalCacheDir() != null) {
            fileName = getExternalCacheDir().getAbsolutePath();
        }

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(getString(R.string.format_yyyyMMdd_HHmmss), Locale.getDefault());
        String formattedDate = df.format(c.getTime());

        LogUtils.e("date Formatted: " + formattedDate.replaceAll("-", "").replaceAll(":", "").replace(" ", ""));
        fileName += "/AUD-" + formattedDate.replaceAll("-", "").replaceAll(":", "").replace(" ", "") + ".3gp";
        mediaRecorder.setOutputFile(fileName);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(96000);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            LogUtils.e("Erro ao preparar inicio de gravação de audio.", e);
        }

        recordAudioTimer = new Timer();
        UpdateAudioTimeTimerTask uatTimerTask = new UpdateAudioTimeTimerTask();
        recordAudioTimer.schedule(uatTimerTask, 1000, 1000);

        imAudioRec.setAnimation(animation);
        vibrate();
        mediaRecorder.start();
    }

    private void stopRecord(boolean sendAudio) {
        // return to initial location
        viewPropertyAnimator.translationX(0).start();

        ViewCompat.animate(imSendAudio).scaleX(1f).scaleY(1f).setDuration(1).start();

        if (recordAudioTimer != null) {
            recordAudioTimer.cancel();
        }

        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                if (recordTimeText.getText().equals(getString(R.string.zero_time))) {
                    audioView.setVisibility(View.GONE);
                    messageView.setVisibility(View.VISIBLE);
                    return;
                }
            } catch (RuntimeException e) {
                sendAudio = false;
            }
        }

        recordTimeText.setText(getString(R.string.zero_time));
        vibrate();

        audioView.setVisibility(View.GONE);
        messageView.setVisibility(View.VISIBLE);

        if (sendAudio) {
            File audioFile = new File(fileName);
            sendAudioMessage(audioFile);
        }

        etMessage.requestFocus();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(100);
        }
    }

    private static int dp(float value) {
        return (int) Math.ceil(1 * value);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (viewMsgSelected.getVisibility() == View.VISIBLE) {
            restoreActionBar();
            adapter.formatSelectedStates();
            adapter.notifyDataSetChanged();
        } else {
            super.onBackPressed();
        }
    }

    private void initGroupChannel() {
        GroupChannel.getChannel(channelUrl, new GroupChannel.GroupChannelGetHandler() {
            @Override
            public void onResult(GroupChannel groupChannel, SendBirdException e) {
                if (e != null) {
                    LogUtils.e("Erro ao iniciar chat. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                    dismissProgress();
                    Toast.makeText(ChatActivity.this, "Não foi possível iniciar conversa. Por favor, tente novamente.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                mGroupChannel = groupChannel;
                setTotalUnreadMessages(countUnreadMessages - mGroupChannel.getUnreadMessageCount());


                opponentId = SendbirdHelper.getOpponentId(groupChannel);
                loadOpponentAvatar(opponentId);


                String opponentName = SendbirdHelper.getOpponentNickname(groupChannel);
                tvUserName.setText(opponentName);

                mGroupChannel.markAsRead();

                adapter.setGroupChannel(mGroupChannel);
                adapter.setOpponentNickName(opponentName);
                adapter.notifyDataSetChanged();

                tvLastSeen = (TextView) customView.findViewById(R.id.actionBar_tvOnline);
                User.ConnectionStatus connectionStatus = SendbirdHelper.getInstance().getOpponentStatus(mGroupChannel);

                if (connectionStatus == User.ConnectionStatus.ONLINE) {
                   tvLastSeen.setText("Online");
                } else {
                    long opponentLastSeen = SendbirdHelper.getInstance().getOpponentLastSeenAt(mGroupChannel);
                    tvLastSeen.setText(TimeUtils.getTimeAgo(opponentLastSeen, ChatActivity.this));
                }

                restoreActionBar();

                loadPrevMessages(true);
            }
        });
    }

    private void loadOpponentAvatar(final String opponentId) {

        if (PreferenceManager.getInstance().getOpponentAvatar(opponentId) != null){
            if (!PreferenceManager.getInstance().getOpponentAvatar(opponentId).isEmpty()){
                Picasso.with(ChatActivity.this)
                        .load(PreferenceManager.getInstance().getOpponentAvatar(opponentId))
                        .fit()
                        .error(R.drawable.placeholder_avatar)
                        .placeholder(R.drawable.placeholder_avatar)
                        .into(imAvatar);
            }
        }

        FirebaseStorage mFirebaseStorage;
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH)
                .child(opponentId).getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.e("photo", uri.toString());
                        Picasso.with(ChatActivity.this)
                                .load(uri.toString())
                                .fit()
                                .error(R.drawable.placeholder_avatar)
                                .placeholder(R.drawable.placeholder_avatar)
                                .into(imAvatar);
                        PreferenceManager.getInstance().setOpponentAvatar(opponentId, uri.toString());
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        });
    }

    private void refreshGroupChannel() {
        if (mGroupChannel == null) {
            return;
        }

        mGroupChannel.markAsRead();
        mGroupChannel.refresh(new GroupChannel.GroupChannelRefreshHandler() {
            @Override
            public void onResult(SendBirdException e) {
                adapter.notifyDataSetChanged();
                restoreActionBar();
                loadPrevMessages(true);
            }
        });
    }

    private void loadPrevMessages(final boolean refresh) {
        if (mGroupChannel == null) {
            dismissProgress();
            return;
        }

        if (refresh || mPrevMessageListQuery == null) {
            mPrevMessageListQuery = mGroupChannel.createPreviousMessageListQuery();
        }

        if (mPrevMessageListQuery.isLoading() || !mPrevMessageListQuery.hasMore()) {
            dismissProgress();
            return;
        }

        mPrevMessageListQuery.load(50, true, new PreviousMessageListQuery.MessageListQueryResult() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if (e != null) {
                    LogUtils.e("Erro ao carregar lista de conversas. ConnectionState: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                    dismissProgress();
                    return;
                }

                if (refresh) {
                    adapter.clear();
                }

                sendForwardedMessages();

                for (BaseMessage message : list) {
                    adapter.insertMessage(message);
                }
                adapter.notifyDataSetChanged();
                listView.setSelection(list.size());

                dismissProgress();
            }
        });
    }

    private void sendForwardedMessages() {
        if (forwardedMessages != null && !forwardedMessages.isEmpty()) {
            for (SelectedMessage message : forwardedMessages) {
                if (message.getObjClass() == FileMessage.class) {
          //          showLoadingDialog();
                    mGroupChannel.sendFileMessage(message.getFileUrl(), message.getFileName(),
                            message.getFileType(), message.getFileSize(), "", message.getCustomType(),
                            new BaseChannel.SendFileMessageHandler() {
                                @Override
                                public void onSent(FileMessage fileMessage, SendBirdException e) {
                                  //  dismissLoadingDialog();
                                    if (e != null) {
                                        LogUtils.e("Erro ao encaminhar arquivo pelo chat. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                                        return;
                                    }

                                    adapter.appendMessage(fileMessage);
                                    adapter.notifyDataSetChanged();
                                }
                            });

                } else if (message.getObjClass() == UserMessage.class) {
                    mGroupChannel.sendUserMessage(message.getMessage(),
                            new BaseChannel.SendUserMessageHandler() {
                                @Override
                                public void onSent(UserMessage userMessage, SendBirdException e) {
                                    if (e != null) {
                                        LogUtils.e("Erro ao encaminhar mensagem pelo chat. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                                        return;
                                    }

                                    adapter.appendMessage(userMessage);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                }
            }
            forwardedMessages.clear();
            forwardedMessages = null;
        }
    }

    private void sendAudioMessage(File audioFile) {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, getString(R.string.sb_send_message), Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioFile == null) {
            Toast.makeText(ChatActivity.this,
                    getString(R.string.could_not_send_audio_message_please_try_again),
                    Toast.LENGTH_LONG).show();
            return;
        }

        final String name = audioFile.getName();
        final String mimeType = Consts.KEY_AUDIO_MESSAGE;
        final int size = Long.valueOf(audioFile.length()).intValue();
        Uri uri = Uri.parse(audioFile.getPath());
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(getBaseContext(), uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        int millis = Integer.parseInt(durationStr);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = (TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
        final String data = String.format(Locale.getDefault(), App.getStr(R.string.format_time_HHmm), minutes, seconds);

        mGroupChannel.sendFileMessage(audioFile, name, mimeType, size, data, null,
                new BaseChannel.SendFileMessageHandler() {
                    @Override
                    public void onSent(FileMessage fileMessage, SendBirdException e) {
                        if (e != null) {
                            LogUtils.e("Erro ao enviar arquivo pelo chat. ConnectionStatus: "
                                    + SendBird.getConnectionState() + ". Erro " + e.getCode()
                                    + ": " + e.getMessage(), e);
                            dismissUploadProgress();
                            Toast.makeText(ChatActivity.this,
                                    getString(R.string.could_not_send_audio_message_please_try_again),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
//                        adapter.addAudioSelectedState();
                        adapter.appendMessage(fileMessage);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void sendFileMessage(final Uri uri) {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, getString(R.string.sb_send_message), Toast.LENGTH_SHORT).show();
            return;
        }

        final File file = ImageUtils.scaleImageFile(this, uri, "chat-upload", 350);
        if (file == null) {
            Toast.makeText(ChatActivity.this,
                    getString(R.string.could_not_send_image_please_try_again),
                    Toast.LENGTH_LONG).show();
            return;
        }

        final String name = file.getName();
        final String mimeType = FileUtils.getMimeType(file.getPath());
        final int size = Long.valueOf(file.length()).intValue();

        mGroupChannel.sendFileMessage(file, name, mimeType, size, "", "", new BaseChannel.SendFileMessageHandler() {
            @Override
            public void onSent(FileMessage fileMessage, SendBirdException e) {
                if (e != null) {
                    LogUtils.e("Erro ao enviar arquivo pelo chat. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                    dismissUploadProgress();
                    Toast.makeText(ChatActivity.this,
                            getString(R.string.could_not_send_image_please_try_again),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                adapter.appendMessage(fileMessage);
                adapter.notifyDataSetChanged();
                dismissUploadProgress();
            }
        });
    }

    private void showUploadProgress() {
        imUpload.setEnabled(false);
        imUpload.setVisibility(View.GONE);
        progressUpload.setVisibility(View.VISIBLE);
    }

    private void dismissUploadProgress() {
        imUpload.setEnabled(true);
        imUpload.setVisibility(View.VISIBLE);
        progressUpload.setVisibility(View.GONE);
    }

    private void sendUserMessage(final String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, getString(R.string.sb_send_message), Toast.LENGTH_SHORT).show();
            return;
        }

        mGroupChannel.sendUserMessage(message, "", "", new BaseChannel.SendUserMessageHandler() {
            @Override
            public void onSent(UserMessage userMessage, SendBirdException e) {
                if (e != null) {
                    LogUtils.e("Erro ao enviar mensagem pelo chat. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ": " + e.getMessage(), e);
                    sendingBadge = false;
                    Toast.makeText(ChatActivity.this,
                            getString(R.string.could_not_send_message_please_try_again),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                adapter.appendMessage(userMessage);
                adapter.notifyDataSetChanged();
                listView.setSelection(listView.getCount());
            }
        });
    }

    private void setTotalUnreadMessages(int unreadMessagesCount) {
        if (unreadMessagesCount > 0) {
            tvCountUnreadChats.setText(String.valueOf(unreadMessagesCount));
            tvCountUnreadChats.setVisibility(View.VISIBLE);
        } else {
            tvCountUnreadChats.setText(null);
            tvCountUnreadChats.setVisibility(View.GONE);
        }
    }

    private void setUpActionBar(int msgsCopied, final BaseMessage message) {
        viewDefault.setVisibility(View.GONE);
        viewMsgSelected.setVisibility(View.VISIBLE);

        tvCountMsgCopied.setText(String.valueOf(msgsCopied));

        if (message instanceof UserMessage) {
            final UserMessage userMessage = (UserMessage) message;
            if (!userMessage.getSender().getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                imDelete.setVisibility(View.GONE);
            }

        } else {
            final FileMessage fileMessage = (FileMessage) message;
            if (!fileMessage.getSender().getUserId().equals(SendBird.getCurrentUser().getUserId())){
                imDelete.setVisibility(View.GONE);
            }
        }
    }

    private void restoreActionBar() {
        adapter.formatSelectedStates();
        adapter.notifyDataSetChanged();
        messagesCopied.clear();

        viewDefault.setVisibility(View.VISIBLE);
        viewMsgSelected.setVisibility(View.GONE);
        imDelete.setVisibility(View.VISIBLE);
        imCopy.setVisibility(View.VISIBLE);
    }

    private ArrayList<SelectedMessage> getCopiedMessages() {
        // Short list by date
        ArrayList<BaseMessage> baseMessages = new ArrayList<>();
        for (int i = 0; i < messagesCopied.size(); i++) {
            long key = messagesCopied.keyAt(i);
            baseMessages.add(messagesCopied.get(key));
        }
        Collections.sort(baseMessages, new Comparator<BaseMessage>() {
            @Override
            public int compare(BaseMessage o1, BaseMessage o2) {
                return Long.valueOf(o1.getCreatedAt()).compareTo(o2.getCreatedAt());
            }
        });
        // Extract messagesCopied
        ArrayList<SelectedMessage> copiedMessages = new ArrayList<>();
        for (BaseMessage baseMessage : baseMessages) {
            if (baseMessage instanceof FileMessage) {
                FileMessage fileMessage = (FileMessage) baseMessage;
                SelectedMessage forwardedMessage = new SelectedMessage(fileMessage);
                copiedMessages.add(forwardedMessage);

            } else if (baseMessage instanceof UserMessage) {
                UserMessage useMessage = (UserMessage) baseMessage;
                SelectedMessage userMessage = new SelectedMessage(useMessage);
                copiedMessages.add(userMessage);
            }
        }

        return copiedMessages;
    }

    private void copyMessageToClipboard(ArrayList<SelectedMessage> messages) {
        if (messages != null && !messages.isEmpty()) {
            StringBuilder selectedText = new StringBuilder();

            for (SelectedMessage message : messages) {
                if (message.getObjClass() == FileMessage.class) {
                    if (selectedText.length() > 0) {
                        selectedText.append("\n");
                    }
                    selectedText.append(message.getFileName());

                } else if (message.getObjClass() == UserMessage.class) {
                    if (selectedText.length() > 0) {
                        selectedText.append("\n");
                    }
                    selectedText.append(message.getMessage());
                }
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.app_name), selectedText.toString());
            if (clipboard != null && clip != null) {
                clipboard.setPrimaryClip(clip);
            }
        }
    }

    private void showSelectImagePopup() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.pick_image))
                .items(R.array.upload_array_list)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which) {
                            case 0: // From camera
                                if (ActivityCompat.checkSelfPermission(ChatActivity.this,
                                        android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                                        || ActivityCompat.checkSelfPermission(ChatActivity.this,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(ChatActivity.this,
                                            new String[] {android.Manifest.permission.CAMERA,
                                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            Consts.REQUEST_CODE_PERMISSION_CAMERA_IMAGE);
                                } else {
                                    mIsUploading = true;
                                    currentPhotoUri = App.getInstance().requestCaptureImageFromCamera(
                                            ChatActivity.this,
                                            Consts.REQUEST_CODE_IMAGE_FROM_CAMERA, "image");
                                }
                                break;

                            case 1: // From gallery
                                if(ActivityCompat.checkSelfPermission(ChatActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                                    ActivityCompat.requestPermissions(ChatActivity.this, new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                                    android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                            Consts.REQUEST_CODE_PERMISSION_GALLERY_IMAGE);
                                } else {
                                    mIsUploading = true;
                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    intent.setType("image/*");
                                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                                    }
                                    startActivityForResult(intent, Consts.REQUEST_CODE_IMAGE_FROM_GALLERY);
                                }
                                break;

                            default:
                                break;
                        }
                    }
                })
                .show();
    }

    private void showProgress() {
        progressLoading.setVisibility(View.VISIBLE);
        imUpload.setEnabled(false);
        imUpload.setClickable(false);
        etMessage.setEnabled(false);
    }

    private void dismissProgress() {
        imUpload.setEnabled(true);
        imUpload.setClickable(true);
        etMessage.setEnabled(true);
        progressLoading.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

//        boolean hasPermissions = true;
//        if (grantResults.length > 0) {
//            for (int grantResult : grantResults) {
//                if (grantResult != PackageManager.PERMISSION_GRANTED) {
//                    hasPermissions = false;
//                    break;
//                }
//            }
//        } else {
//            hasPermissions = false;
//        }

        switch (requestCode) {
            case Consts.REQUEST_CODE_PERMISSION_CAMERA_IMAGE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mIsUploading = true;
                    currentPhotoUri = App.getInstance().requestCaptureImageFromCamera(ChatActivity.this, Consts.REQUEST_CODE_IMAGE_FROM_CAMERA, "image");
                }
                break;
            case Consts.REQUEST_CODE_PERMISSION_GALLERY_IMAGE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mIsUploading = true;
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    startActivityForResult(intent, Consts.REQUEST_CODE_IMAGE_FROM_GALLERY);
                }
                break;
            case Consts.REQUEST_CODE_PERMISSION_AUDIO:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtils.e("permission granted!");
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Consts.REQUEST_CODE_IMAGE_FROM_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                if (currentPhotoUri != null) {
                    showUploadProgress();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendFileMessage(currentPhotoUri);
                        }
                    }, 2000);
                }
            }

        } else if (requestCode == Consts.REQUEST_CODE_IMAGE_FROM_GALLERY) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (data.getData() != null) {
                        showUploadProgress();
                        sendFileMessage(data.getData());

                    } else if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                        showUploadProgress();
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            final ClipData.Item item = clipData.getItemAt(i);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendFileMessage(item.getUri());
                                }
                            }, 2000);
                        }
                    }

                } else {
                    if (data.getData() != null) {
                        showUploadProgress();
                        sendFileMessage(data.getData());
                    }
                }
            }
        }
    }

    @Override
    public void onConnectSucceeded() {
        SendbirdHelper.removeConnectListener(this);
        initGroupChannel();
    }

    @Override
    public void onConnectFailed() {
        SendbirdHelper.removeConnectListener(this);
        initGroupChannel();
    }

    @Override
    public void onAvatarsUrlLoaded() {
    }

    public static void sendUserMessage(final String message, String groupChannelUrl) {
        GroupChannel.getChannel(groupChannelUrl, new GroupChannel.GroupChannelGetHandler() {
            @Override
            public void onResult(final GroupChannel groupChannel, SendBirdException e) {
                SendbirdHelper.init(App.getContext());

                if (SendBird.getCurrentUser() == null) {
                    SendBird.connect(String.valueOf(PreferenceManager.getInstance().getUserId()),
                            new SendBird.ConnectHandler() {
                                @Override
                                public void onConnected(final User user, SendBirdException e) {
                                    LogUtils.i("SendbirdHelper.connect >> onConnected");
                                    if (e != null) {
                                        LogUtils.e("SendbirdHelper.connect >> Erro ao conectar-se. Erro " + e.getCode() + ": " + e.getMessage(), e);
                                        return;
                                    }

                                    groupChannel.sendUserMessage(message, null);
                                }
                            });
                }
            }
        });
    }

    @Override
    public void onPlayAudio(final MessagesAdapter.AudioMessageViewHolder holder, final MediaPlayer mediaPlayer, final Timer timer, boolean isOpponentMessage) {
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        if (!isOpponentMessage) {
            holder.audioProgressBar.setMax(mediaPlayer.getDuration());

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    timer.cancel();
                    holder.imPlay.setImageResource(R.drawable.ic_play_audio);
                }
            });

            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                holder.imPlay.setImageResource(R.drawable.ic_pause_audio);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (mediaPlayer.isPlaying()) {
                            holder.audioProgressBar.setProgress(mediaPlayer.getCurrentPosition());
                        }
                    }
                }, 0, 100);

            } else {
                mediaPlayer.pause();
                holder.imPlay.setImageResource(R.drawable.ic_play_audio);
                timer.cancel();
            }

        } else {
            holder.audioOpponentBar.setMax(mediaPlayer.getDuration());

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    timer.cancel();
                    holder.imOpponentPlay.setImageResource(R.drawable.ic_play_audio);
                }
            });

            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                holder.imOpponentPlay.setImageResource(R.drawable.ic_pause_audio);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (mediaPlayer.isPlaying()) {
                            holder.audioOpponentBar.setProgress(mediaPlayer.getCurrentPosition());
                        }
                    }
                }, 0, 100);

            } else {
                mediaPlayer.pause();
                holder.imOpponentPlay.setImageResource(R.drawable.ic_play_audio);
                timer.cancel();
            }
        }
    }

    private class UpdateAudioTimeTimerTask extends TimerTask {

        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            long hours = TimeUnit.MILLISECONDS.toMinutes(updatedTime)
                    - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(updatedTime));
            long minutes = TimeUnit.MILLISECONDS.toSeconds(updatedTime)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(updatedTime));
            final String formattedTime = String.format(getString(R.string.format_time_HHmm),
                    hours, minutes);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recordTimeText != null) {
                        recordTimeText.setText(formattedTime);
                    }
                }
            });
        }
    }

}