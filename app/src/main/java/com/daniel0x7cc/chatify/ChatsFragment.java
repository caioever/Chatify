package com.daniel0x7cc.chatify;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daniel0x7cc.chatify.adapters.ChatsAdapter;
import com.daniel0x7cc.chatify.interfaces.OnSendbirdConnectedListener;
import com.daniel0x7cc.chatify.interfaces.SendBirdEventListener;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.daniel0x7cc.chatify.helpers.SendbirdHelper;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.GroupChannelListQuery;
import com.sendbird.android.OpenChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

import java.util.List;

public class ChatsFragment extends Fragment implements OnSendbirdConnectedListener, SendBirdEventListener {

    private static final String identifier = "SendBirdGroupChannelList";
    private ListView mListView;
    private ChatsAdapter mAdapter;
    private GroupChannelListQuery mQuery;
    private ProgressBar progressBar;
    private TextView tvEmptyMsg;
    RelativeLayout loadingLayout;
    private long mLastClickTime = 0;

    public ChatsFragment() {
    }

    public static ChatsFragment newInstance() {
        return new ChatsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = (ProgressBar) view.findViewById(R.id.fraChats_progress);
        progressBar.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(getContext(), R.color.main_color),
                PorterDuff.Mode.SRC_IN);

        // tvEmptyMsg = (TextView) view.findViewById(R.id.fragChats_emptyMsg);
        loadingLayout = (RelativeLayout) view.findViewById(R.id.fraChats_loading);
        mListView = (ListView) view.findViewById(R.id.fraChats_listView);
        mAdapter = new ChatsAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        mListView.setEmptyView(view.findViewById(R.id.fraChats_viewEmpty));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                GroupChannel channel = mAdapter.getItem(position);
                String opponentId = SendbirdHelper.getOpponentId(channel);
                String opponentName = SendbirdHelper.getOpponentNickname(channel);
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra(Consts.KEY_CHAT_CHANNEL_URL, channel.getUrl());
                intent.putExtra(Consts.KEY_CHAT_OPONNENT_ID, opponentId);
                intent.putExtra(Consts.KEY_CHAT_OPONNENT_NAME, opponentName);
                startActivity(intent);
            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount >= (int) (totalItemCount * 0.8f)) {
                    loadNextChannels(false);
                }
            }
        });


        showProgress();
    }

    @Override
    public void onSendbirdConnected() {
        loadNextChannels(false);
    }

    private void initUI(View rootView) {


//        // Open chat
//        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//                GroupChannel channel = mAdapter.getItem(position);
//                Intent it = new Intent(getActivity(), ChatActivity.class);
//                it.putExtra("channel_url", channel.getUrl());
//                startActivity(it);
//            }
//        });
//
//        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long id) {
//
//                final GroupChannel channel = mAdapter.getItem(position);
//
//                new AlertDialog.Builder(getActivity())
//                        .setTitle("Deletar")
//                        .setMessage("Você deseja excluir esta conversa?")
//                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                channel.leave(new GroupChannel.GroupChannelLeaveHandler() {
//                                    @Override
//                                    public void onResult(SendBirdException e) {
//                                        if(e != null) {
//                                            Toast.makeText(getActivity(), "" + e.getCode() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                            return;
//                                        }
//
//                                        mAdapter.remove(position);
//                                        mAdapter.notifyDataSetChanged();
//                                    }
//                                });
//                            }
//                        })
//                        .setNeutralButton("Arquivar", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                channel.hide(new GroupChannel.GroupChannelHideHandler() {
//                                    @Override
//                                    public void onResult(SendBirdException e) {
//                                        if(e != null) {
//                                            Toast.makeText(getActivity(), "" + e.getCode() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                            return;
//                                        }
//
//                                        mAdapter.remove(position);
//                                        mAdapter.notifyDataSetChanged();
//                                    }
//                                });
//
//                            }
//                        })
//                        .setNegativeButton("Não", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                            }
//                        }).create().show();
//                return true;
//            }
//        });
//
//        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//
//            }
//
//            @Override
//            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                if (firstVisibleItem + visibleItemCount >= (int) (totalItemCount * 0.8f)) {
//                    loadNextChannels();
//                }
//            }
//        });


        mAdapter = new ChatsAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        showProgress();
    }

    private void loadNextChannels(final boolean reload) {
        SendBird.ConnectionState connectionStatus = SendBird.getConnectionState();
        if (connectionStatus == SendBird.ConnectionState.OPEN) {
            if (mQuery == null || reload) {
                mQuery = GroupChannel.createMyGroupChannelListQuery();
                mQuery.setIncludeEmpty(false);
            }
        } else {
            SendbirdHelper.addConnectListener(this);
            return;
        }

        if (mQuery == null || mQuery.isLoading()) {
            dismissProgress();
            return;
        }

        if (!mQuery.hasNext()) {
            dismissProgress();
            return;
        }

        mQuery.next(new GroupChannelListQuery.GroupChannelListQueryResultHandler() {
            @Override
            public void onResult(List<GroupChannel> list, SendBirdException e) {
                //  dismissProgress();
                if (e != null) {
                    LogUtils.e("Erro ao obter lista de conversas. ConnectionStatus: " + SendBird.getConnectionState() + ". Erro " + e.getCode() + ":" + e.getMessage());
                    return;
                }

                if (reload) {
                    mAdapter.clear();
                }

                mAdapter.addAll(list);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showProgress() {
        loadingLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        //  tvEmptyMsg.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
    }

    private void dismissProgress() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            progressBar.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.GONE);
            // tvEmptyMsg.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SendBird.addChannelHandler(identifier, new SendBird.ChannelHandler() {
            @Override
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                if (baseChannel instanceof GroupChannel) {
                    GroupChannel groupChannel = (GroupChannel) baseChannel;
                    mAdapter.replace(groupChannel);
                }
            }

            @Override
            public void onUserEntered(OpenChannel channel, User user) {
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onUserExited(OpenChannel channel, User user) {
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onUserJoined(GroupChannel groupChannel, User user) {
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onUserLeft(GroupChannel groupChannel, User user) {
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onTypingStatusUpdated(GroupChannel channel) {
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChannelChanged(BaseChannel channel) {
                if (channel instanceof GroupChannel) {
                    GroupChannel groupChannel = (GroupChannel) channel;
                    mAdapter.replace(groupChannel);
                }
            }
        });

        loadNextChannels(true);
    }

    @Override
    public void onConnectSucceeded() {

    }

    @Override
    public void onConnectFailed() {

    }

    @Override
    public void onAvatarsUrlLoaded() {

    }
}
