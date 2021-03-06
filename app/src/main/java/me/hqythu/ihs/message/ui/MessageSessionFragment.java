package me.hqythu.ihs.message.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.ihs.message_2012010548.managers.HSMessageManager;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import me.hqythu.ihs.message.R;
import me.hqythu.ihs.message.data.MessageSession;
import me.hqythu.ihs.message.data.MessageSessionType;
import me.hqythu.ihs.message.db.SessionDBManager;
import me.hqythu.ihs.message.event.FriendUpdateEvent;
import me.hqythu.ihs.message.event.SessionDeleteEvent;
import me.hqythu.ihs.message.event.SessionStatusChangeEvent;
import me.hqythu.ihs.message.event.SessionUnreadCountChangeEvent;
import me.hqythu.ihs.message.event.SessionUpdateEvent;

/**
 * Created by hqythu on 9/8/2015.
 */
public class MessageSessionFragment extends Fragment {

    private static final String SAVED_STATE_EXPANDABLE_ITEM_MANAGER = "RecyclerViewExpandableItemManager";

    private ArrayList<MessageSession> mSessionInfos;
    private RecyclerView mSessionList;
    private MessageSessionAdapter mAdapter;

    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    public static final String DISPLAY_TYPE = "DisplayType";

    public static final int SESSION_LIST_TYPE_ALL = 1;
    public static final int SESSION_LIST_TYPE_INBOX = 2;
    public static final int SESSION_LIST_TYPE_ARCHIVED = 3;
    public static final int SESSION_LIST_TYPE_SNOOZED = 4;

    public int displayType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        displayType = bundle.getInt(DISPLAY_TYPE);

        ArrayList<SessionDBManager.MessageSessionInfo> sessions =
            SessionDBManager.getSessionInfoList(displayType);
        mSessionInfos = new ArrayList<>();
        for (SessionDBManager.MessageSessionInfo session : sessions) {
            mSessionInfos.add(new MessageSession(session));
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_message_session, null);

        mSessionList = (RecyclerView) view.findViewById(R.id.session_list);
        mAdapter = new MessageSessionAdapter(getActivity(), mSessionInfos, displayType);
        mSessionList.setAdapter(mAdapter);
        mSessionList.setLayoutManager(new LinearLayoutManager(getActivity()));

        final Parcelable eimSavedState = (savedInstanceState != null) ? savedInstanceState.getParcelable(SAVED_STATE_EXPANDABLE_ITEM_MANAGER) : null;

        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);

        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();
        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(mAdapter);

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        mSessionList.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        mSessionList.setItemAnimator(animator);
        mSessionList.setHasFixedSize(false);

        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mSessionList);
        mRecyclerViewSwipeManager.attachRecyclerView(mSessionList);

        return view;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEvent(SessionUpdateEvent event) {
        int position = mSessionInfos.indexOf(event.getSession());
        if (position == -1) {
            if (displayType == SESSION_LIST_TYPE_INBOX) {
                mSessionInfos.add(0, event.getSession());
                mWrappedAdapter.notifyItemInserted(0);
            }
        } else {
            mSessionInfos.remove(position);
            mSessionInfos.add(0, event.getSession());
            mWrappedAdapter.notifyItemMoved(position, 0);
            mWrappedAdapter.notifyItemChanged(0);
        }
    }

    public void onEvent(SessionDeleteEvent event) {
        int position = mSessionInfos.indexOf(event.getSession());
        mSessionInfos.remove(position);
        mWrappedAdapter.notifyItemRemoved(position);
    }

    public void onEvent(SessionStatusChangeEvent event) {
        if (displayType == SESSION_LIST_TYPE_ARCHIVED) {
            if (event.getType() == MessageSessionType.TYPE_ARCHIVED) {
                mSessionInfos.add(0, event.getSession());
                mWrappedAdapter.notifyItemInserted(0);
            }
        } else if (displayType == SESSION_LIST_TYPE_INBOX) {
            if (event.getType() == MessageSessionType.TYPE_INBOX) {
                mSessionInfos.add(0, event.getSession());
                mWrappedAdapter.notifyItemInserted(0);
            }
        } else if (displayType == SESSION_LIST_TYPE_SNOOZED) {
            if (event.getType() == MessageSessionType.TYPE_SNOOZED) {
                mSessionInfos.add(0, event.getSession());
                mWrappedAdapter.notifyItemInserted(0);
            }
        }
    }

    public void onEvent(SessionUnreadCountChangeEvent event) {
        int count = 0;
        for (MessageSession session : mSessionInfos) {
            if (session.contactMid.equals(event.getContactMid())) {
                session.unreadCount = event.getUnreadCount();
                mWrappedAdapter.notifyItemChanged(count);
                count++;
            }
        }
    }

    public void onEvent(FriendUpdateEvent event) {
        for (MessageSession session : mSessionInfos) {
            session.updateContact();
        }
        mWrappedAdapter.notifyDataSetChanged();
    }
}
