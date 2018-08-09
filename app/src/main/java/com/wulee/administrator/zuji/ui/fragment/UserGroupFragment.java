package com.wulee.administrator.zuji.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.stetho.common.LogUtil;
import com.jude.easyrecyclerview.EasyRecyclerView;
import com.wulee.administrator.zuji.R;
import com.wulee.administrator.zuji.adapter.UserGroupAdapter;
import com.wulee.administrator.zuji.chatui.ui.activity.ChatMainActivity;
import com.wulee.administrator.zuji.database.bean.PersonInfo;
import com.wulee.administrator.zuji.ui.NearUserActivity;
import com.wulee.administrator.zuji.utils.OtherUtil;
import com.wulee.administrator.zuji.widget.BaseTitleLayout;
import com.wulee.administrator.zuji.widget.RecycleViewDivider;
import com.wulee.administrator.zuji.widget.TitleLayoutClickListener;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import cn.bmob.newim.BmobIM;
import cn.bmob.newim.bean.BmobIMConversation;
import cn.bmob.newim.bean.BmobIMUserInfo;
import cn.bmob.newim.core.ConnectionStatus;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

/**
 * Created by wulee on 2017/12/6 09:52
 */
public class UserGroupFragment extends MainBaseFrag {


    @InjectView(R.id.titlelayout)
    BaseTitleLayout titlelayout;
    @InjectView(R.id.recyclerview)
    EasyRecyclerView recyclerview;
    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;
    private View mRootView;
    private Context mContext;

    private static final int STATE_REFRESH = 0;// 下拉刷新
    private static final int STATE_MORE = 1;// 加载更多
    private int PAGE_SIZE = 10;
    private int curPage = 0;
    private boolean isRefresh = false;

    private UserGroupAdapter mAdapter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.user_group_list_main, container, false);
        }
        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeView(mRootView);
        }
        ButterKnife.inject(this, mRootView);
        initView(mRootView);
        return mRootView;
    }

    private void initView(View rootView) {
        ImageView topHeaderIv = (ImageView) rootView.findViewById(R.id.ivstatebar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            topHeaderIv.setVisibility(View.VISIBLE);
        } else {
            topHeaderIv.setVisibility(View.GONE);
        }
        swipeLayout.setColorSchemeResources(R.color.left_menu_bg,R.color.colorAccent);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new UserGroupAdapter(R.layout.user_group_list_item,null,mContext);
        recyclerview.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerview.addItemDecoration(new RecycleViewDivider(mContext, LinearLayoutManager.HORIZONTAL, 1, ContextCompat.getColor(mContext, R.color.grayline)));
        recyclerview.setAdapter(mAdapter);

        addListener();
    }


    private void addListener() {
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            List<PersonInfo> piInfoList = mAdapter.getData();
            if(null != piInfoList && piInfoList.size()>0){
                PersonInfo personInfo = piInfoList.get(position);
                if(null != personInfo){

                    //chat(personInfo.getObjectId(),personInfo.getUsername(),personInfo.getHeader_img_url());
                }
            }
        });

        swipeLayout.setOnRefreshListener(() -> {
            isRefresh = true;
            curPage = 0;
            getUserList(curPage, STATE_REFRESH);
        });
        //加载更多
        mAdapter.setEnableLoadMore(true);
        mAdapter.setPreLoadNumber(PAGE_SIZE);
        mAdapter.setOnLoadMoreListener(() -> getUserList(curPage, STATE_MORE));

        titlelayout.setOnTitleClickListener(new TitleLayoutClickListener() {
            @Override
            public void onRightImg1ClickListener() {
               startActivity(new Intent(mContext, NearUserActivity.class));
            }
        });

        recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 查看源码可知State有三种状态：SCROLL_STATE_IDLE（静止）、SCROLL_STATE_DRAGGING（上升）、SCROLL_STATE_SETTLING（下落）
                if (newState == SCROLL_STATE_IDLE) { // 滚动静止时才加载数据，极大提升流畅度
                    mAdapter.setScrolling(false);
                    mAdapter.notifyDataSetChanged(); // notify调用后onBindViewHolder会响应调用
                } else
                    mAdapter.setScrolling(true);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    /**
     * 与陌生人聊天
     */
    private void chat(String objectId,String name,String avatar) {
        if (BmobIM.getInstance().getCurrentStatus().getCode() != ConnectionStatus.CONNECTED.getCode()) {
            OtherUtil.showToastText("尚未连接IM服务器");
            return;
        }
        Intent intent = new Intent(mContext,ChatMainActivity.class);
        //创建一个常态会话入口，陌生人聊天
        BmobIMUserInfo info  = new BmobIMUserInfo(objectId, name, avatar);
        BmobIMConversation conversationEntrance = BmobIM.getInstance().startPrivateConversation(info, null);
        Bundle bundle = new Bundle();
        bundle.putSerializable("c", conversationEntrance);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onFragmentFirstSelected() {
        showProgressDialog(getActivity(),true);
        getUserList(0,STATE_REFRESH);
    }


    /**
     * 分页获取数据
     */
    private void getUserList(final int page, final int actionType){
        BmobQuery<PersonInfo> query = new BmobQuery<>();
        query.order("-createdAt");
        // 如果是加载更多
        if(actionType == STATE_MORE){
            // 跳过之前页数并去掉重复数据
            query.setSkip(page * PAGE_SIZE + 1);
        }else{
            query.setSkip(0);
        }
        // 设置每页数据个数
        query.setLimit(PAGE_SIZE);
        query.findObjects(new FindListener<PersonInfo>() {
            @Override
            public void done(List<PersonInfo> dataList, BmobException e) {
                stopProgressDialog();
                if (swipeLayout != null && swipeLayout.isRefreshing()){
                    swipeLayout.setRefreshing(false);
                }
                if(e == null){
                    curPage++;
                    if (isRefresh){//下拉刷新需清理缓存
                        mAdapter.setNewData(dataList);
                        isRefresh = false;
                    }else {//正常请求 或 上拉加载更多时处理流程
                        if (dataList.size() > 0) {
                            mAdapter.addData(dataList);
                            mAdapter.loadMoreComplete();
                        }else {
                            mAdapter.loadMoreEnd();
                        }
                    }
                }else{
                    mAdapter.loadMoreFail();
                    LogUtil.d("查询UserInfo失败"+e.getMessage()+","+e.getErrorCode());
                }
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }
}