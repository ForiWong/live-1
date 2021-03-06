package com.angelatech.yeyelive.fragment;

import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.angelatech.yeyelive.CommonUrlConfig;
import com.angelatech.yeyelive.Constant;
import com.angelatech.yeyelive.R;
import com.angelatech.yeyelive.activity.function.FocusFans;
import com.angelatech.yeyelive.adapter.CommonAdapter;
import com.angelatech.yeyelive.adapter.ViewHolder;
import com.angelatech.yeyelive.db.model.BasicUserInfoDBModel;
import com.angelatech.yeyelive.model.BasicUserInfoModel;
import com.angelatech.yeyelive.model.CommonModel;
import com.angelatech.yeyelive.model.CommonParseListModel;
import com.angelatech.yeyelive.model.FocusModel;
import com.angelatech.yeyelive.util.CacheDataManager;
import com.angelatech.yeyelive.util.JsonUtil;
import com.angelatech.yeyelive.view.LoadingDialog;
import com.angelatech.yeyelive.web.HttpFunction;
import com.google.gson.reflect.TypeToken;
import com.will.common.log.DebugLogs;
import com.will.view.ToastUtils;
import com.will.view.library.SwipyRefreshLayout;
import com.will.view.library.SwipyRefreshLayoutDirection;
import com.will.web.handle.HttpBusinessCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关系fragment(包括粉丝和关注
 */
public class RelationFragment extends BaseFragment implements SwipyRefreshLayout.OnRefreshListener {

    private View view;
    private ListView list_view_focus;
    private CommonAdapter<FocusModel> adapter;
    private List<FocusModel> data = new ArrayList<>();
    private int pageIndex = 1;
    private int pageSize = 10;
    private int type = FocusFans.TYPE_FANS;
    private long dateSort = 0;
    private boolean IS_REFRESH = false;  //是否需要刷新
    private FocusFans focusFans;
    private final int MSG_ADAPTER_NOTIFY = 1;
    private final int MSG_SET_FOLLOW = 2;
    private final int MSG_NO_DATA = 3;
    private SwipyRefreshLayout swipyRefreshLayout;

    private String url = CommonUrlConfig.FriendHeList;
    private String fuserid;
    private BasicUserInfoDBModel model;
    private RelativeLayout noDataLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_relation, container, false);
        initView();
        setView();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void initView() {
        list_view_focus = (ListView) view.findViewById(R.id.list_view_focus);
        swipyRefreshLayout = (SwipyRefreshLayout) view.findViewById(R.id.pullToRefreshView);
        noDataLayout = (RelativeLayout) view.findViewById(R.id.no_data_layout);
        focusFans = new FocusFans(getActivity());
        adapter = new CommonAdapter<FocusModel>(getContext(), data, R.layout.item_focus) {
            @Override
            public void convert(ViewHolder helper, final FocusModel item, final int position) {
                helper.setImageViewByImageLoader(R.id.user_head_photo, item.headurl);
                helper.setText(R.id.tv_name, item.nickname);
                if (item.sex.equals(Constant.SEX_MALE)) {
                    helper.setImageResource(R.id.iv_user_sex, R.drawable.icon_information_boy);
                } else {
                    helper.setImageResource(R.id.iv_user_sex, R.drawable.icon_information_girl);
                }
                if (item.isfollow.equals("0")) {
                    helper.setImageResource(R.id.iv_user_follow_state, R.drawable.btn_focus);
                } else {
                    helper.setImageResource(R.id.iv_user_follow_state, R.drawable.btn_focus_cancel);
                }
                if (item.isv.equals("1")) {
                    helper.showView(R.id.iv_vip);
                } else {
                    helper.hideView(R.id.iv_vip);
                }
                helper.setOnClick(R.id.iv_user_follow_state, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doFocus(data.get(position), position);
                    }
                });
            }
        };

    }

    private void setView() {
        model = CacheDataManager.getInstance().loadUser();
        list_view_focus.setAdapter(adapter);
        swipyRefreshLayout.setOnRefreshListener(this);
        swipyRefreshLayout.setDirection(SwipyRefreshLayoutDirection.BOTTOM);
        list_view_focus.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FocusModel focusModel = data.get(position);
                BasicUserInfoModel userInfoModel = new BasicUserInfoModel();
                userInfoModel.Userid = focusModel.userid;
                userInfoModel.isfollow = focusModel.isfollow;
                userInfoModel.headurl = focusModel.headurl;
                userInfoModel.nickname = focusModel.nickname;
                userInfoModel.isv = focusModel.isv;
                userInfoModel.sex = focusModel.sex;
                UserInfoDialogFragment userInfoDialogFragment = new UserInfoDialogFragment();
                userInfoDialogFragment.setUserInfoModel(userInfoModel);
                userInfoDialogFragment.show(getActivity().getSupportFragmentManager(), "");

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.remove(getParentFragment());
                transaction.commit();

            }
        });
        loadData();
        noDataLayout.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void doHandler(Message msg) {
        switch (msg.what) {
            case MSG_ADAPTER_NOTIFY:
                adapter.notifyDataSetChanged();
                break;
            case MSG_SET_FOLLOW:
                adapter.notifyDataSetChanged();
                ToastUtils.showToast(getActivity(), getString(R.string.success));
                break;
            case MSG_NO_DATA:
                showNodataLayout();
                break;
        }
    }

    private void loadData() {
        LoadingDialog.showLoadingDialog(getActivity());
        HttpBusinessCallback httpCallback = new HttpBusinessCallback() {
            @Override
            public void onFailure(Map<String, ?> errorMap) {

            }

            @Override
            public void onSuccess(String response) {
                CommonParseListModel<FocusModel> result = JsonUtil.fromJson(response, new TypeToken<CommonParseListModel<FocusModel>>() {
                }.getType());
                if (result != null) {
                    if (HttpFunction.isSuc(result.code)) {
                        if (result.data != null && !result.data.isEmpty()) {
                            dateSort = result.time;
                            int index = result.index;
                            if (IS_REFRESH) {
                                data.clear();
                                index = 0;
                            }
                            pageIndex = index + 1;
                            data.addAll(result.data);
                            fragmentHandler.obtainMessage(MSG_ADAPTER_NOTIFY).sendToTarget();
                        }
                    } else {
                        onBusinessFaild(result.code);
                    }
                }
                if (data.isEmpty()) {
                    fragmentHandler.obtainMessage(MSG_NO_DATA).sendToTarget();
                }
                IS_REFRESH = false;
                LoadingDialog.cancelLoadingDialog();
            }
        };

        HashMap<String, String> map = new HashMap<>();
        map.put("token", model.token);
        map.put("userid", model.userid);
        map.put("type", String.valueOf(type));
        map.put("fuserid", fuserid);
        if (dateSort > 0) {
            map.put("datesort", String.valueOf(dateSort));
        }
        map.put("pageindex", String.valueOf(pageIndex));
        map.put("pagesize", String.valueOf(pageSize));
        focusFans.httpGet(url, map, httpCallback);
    }

    private void doFocus(FocusModel userModel, final int position) {

        HttpBusinessCallback callback = new HttpBusinessCallback() {
            @Override
            public void onFailure(Map<String, ?> errorMap) {
                DebugLogs.e("===============失败了");
            }

            @Override
            public void onSuccess(String response) {
                CommonModel results = JsonUtil.fromJson(response, CommonModel.class);

                if (results != null) {
                    if (HttpFunction.isSuc(results.code)) {
                        if (data.get(position).isfollow.equals("1")) {
                            data.get(position).isfollow = "0";
                        } else {
                            data.get(position).isfollow = "1";
                        }
                        fragmentHandler.obtainMessage(MSG_SET_FOLLOW).sendToTarget();
                    } else {
                        onBusinessFaild(results.code);
                    }
                }

            }
        };
        focusFans.UserFollow(CommonUrlConfig.UserFollow, model.token, model.userid, userModel.userid, Integer.valueOf(userModel.isfollow), callback);
    }

    @Override
    public void onRefresh(final SwipyRefreshLayoutDirection direction) {

        if (direction == SwipyRefreshLayoutDirection.TOP) {
            freshLoad();
        } else {
            moreLoad();
        }
        swipyRefreshLayout.setRefreshing(false);

    }

    //加载更多
    private void moreLoad() {
        IS_REFRESH = false;
        loadData();
    }

    //刷新
    private void freshLoad() {
        IS_REFRESH = true;
        loadData();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setFuserid(String fuserid) {
        this.fuserid = fuserid;
    }

    public void setType(int type) {
        this.type = type;
    }

    private void showNodataLayout() {
        noDataLayout.setVisibility(View.VISIBLE);
        noDataLayout.findViewById(R.id.hint_textview1).setVisibility(View.VISIBLE);
        if (type == FocusFans.TYPE_FOCUS) {
            ((TextView) noDataLayout.findViewById(R.id.hint_textview1)).setText(getString(R.string.no_data_no_follow));
        } else {
            ((TextView) noDataLayout.findViewById(R.id.hint_textview1)).setText(getString(R.string.no_data_no_fans));
        }
        noDataLayout.findViewById(R.id.hint_textview2).setVisibility(View.GONE);
    }
}
