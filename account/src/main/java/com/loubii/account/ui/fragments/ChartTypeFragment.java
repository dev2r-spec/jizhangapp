package com.loubii.account.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.LinearLayout;

import com.loubii.account.R;
import com.loubii.account.bean.AccountModel;
import com.loubii.account.constants.Extra;
import com.loubii.account.db.database.DbHelper;
import com.loubii.account.event.ChartYearEvent;
import com.loubii.account.ui.fragments.adapter.BaseFragmentPagerAdapter;
import com.loubii.account.ui.fragments.chart.ChartDetailFragment;
import com.loubii.account.util.AccListUtil;
import com.loubii.account.util.TimeUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.BindView;

/**
 * 图表类型 Fragment (周/月/年容器)
 * 修复了：选中月份与图表数据错位、年份切换刷新问题
 */
public class ChartTypeFragment extends BaseEventFragment {
    private static final String TIME_TYPE = "timeType";
    private static final String SELECT_YEAR = "selectYear";

    public static final int TYPE_WEEK = 1;
    public static final int TYPE_MONTH = 2;
    public static final int TYPE_YEAR = 3;

    @BindView(R.id.tab_dettail) TabLayout mTabDettail;
    @BindView(R.id.vp_chart) ViewPager mVpChart;
    @BindView(R.id.lin_empty) LinearLayout mLinEmpty;

    private int mType;
    private int mSelectedYear;
    private int mAccountType = Extra.ACCOUNT_TYPE_EXPEND;
    private List<AccountModel> mDetailTypeList = new ArrayList<>();

    private ArrayList<String> mTitleList = new ArrayList<>();
    private ArrayList<ChartDetailFragment> mFragmentList = new ArrayList<>();

    public ChartTypeFragment() { }

    public static ChartTypeFragment newInstance(int param1, int year) {
        ChartTypeFragment fragment = new ChartTypeFragment();
        Bundle args = new Bundle();
        args.putInt(TIME_TYPE, param1);
        args.putInt(SELECT_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mType = getArguments().getInt(TIME_TYPE);
            mSelectedYear = getArguments().getInt(SELECT_YEAR, Calendar.getInstance().get(Calendar.YEAR));
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_chart_type;
    }

    @Override
    protected void initData() {
        initChartData();
    }

    @Override
    protected void initView(View view) { }

    /**
     * 【订阅】接收年份切换消息，重置所有子 Fragment
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onYearEvent(ChartYearEvent event) {
        if (this.mSelectedYear == event.year) return;
        this.mSelectedYear = event.year;
        initChartData();
    }

    /**
     * 【修复重点】修正月份索引传递
     */
    private void initChartData() {
        mTitleList.clear();
        mFragmentList.clear();

        Date startOfYear = TimeUtil.getFirstDayOfYear(mSelectedYear);
        Date endOfYear = TimeUtil.getEndDayOfYear(mSelectedYear);
        List<AccountModel> accountList = DbHelper.getInstance().getAccountList(mAccountType, startOfYear, endOfYear);

        if (accountList == null || accountList.isEmpty()) {
            mVpChart.setVisibility(View.GONE);
            mLinEmpty.setVisibility(View.VISIBLE);
            return;
        } else {
            mVpChart.setVisibility(View.VISIBLE);
            mLinEmpty.setVisibility(View.GONE);
        }

        mDetailTypeList = AccListUtil.removeRepeat(accountList);
        float maxValue = getMaxValue(accountList);

        switch (mType) {
            case TYPE_WEEK:
                for (int i = 1; i <= 52; i++) {
                    mTitleList.add("第" + i + "周");
                    mFragmentList.add(ChartDetailFragment.newInstance(TYPE_WEEK, i, maxValue, mSelectedYear));
                }
                break;
            case TYPE_MONTH:
                for (int i = 0; i < 12; i++) {
                    mTitleList.add((i + 1) + "月");
                    // 核心：这里的 i + 1 传入后，ChartDetailFragment 内部必须减 1 对齐 Calendar 标准
                    mFragmentList.add(ChartDetailFragment.newInstance(TYPE_MONTH, i + 1, maxValue, mSelectedYear));
                }
                break;
            case TYPE_YEAR:
                mTitleList.add(mSelectedYear + "年");
                mFragmentList.add(ChartDetailFragment.newInstance(TYPE_YEAR, mSelectedYear, maxValue, mSelectedYear));
                break;
        }

        initViewPager(mFragmentList, mTitleList);
    }

    /**
     * 【核心修复】强制对齐 Tab 和 ViewPager 的选中位置
     */
    private void initViewPager(ArrayList<ChartDetailFragment> mFragmentList, ArrayList<String> mTitleList) {
        BaseFragmentPagerAdapter adapter = new BaseFragmentPagerAdapter(getChildFragmentManager(), mFragmentList, mTitleList);
        mVpChart.setAdapter(adapter);
        mVpChart.setOffscreenPageLimit(1);

        // 1. 计算目标选中的索引 (0-11)
        Calendar cal = Calendar.getInstance();
        int targetPos;
        if (mType == TYPE_MONTH) {
            // 如果是今年则选中当前月，如果是历史年份默认选中12月
            targetPos = (mSelectedYear == cal.get(Calendar.YEAR)) ? cal.get(Calendar.MONTH) : 11;
        } else {
            targetPos = mTitleList.size() - 1;
        }

        // 2. 设置 Tab 模式
        if (mTitleList.size() < 6)
            mTabDettail.setTabMode(TabLayout.MODE_FIXED);
        else
            mTabDettail.setTabMode(TabLayout.MODE_SCROLLABLE);

        // 3. 核心修复：先 setup 再强制指定选中项，防止 setupWithViewPager 重置索引
        mTabDettail.setupWithViewPager(mVpChart);
        mVpChart.setCurrentItem(targetPos, false);

        // 4. 强制 Tab 状态同步
        if (mTabDettail.getTabAt(targetPos) != null) {
            mTabDettail.getTabAt(targetPos).select();
        }
    }

    private float getMaxValue(List<AccountModel> accountList) {
        ArrayList<Float> listFloat = new ArrayList<>();
        if (accountList == null || accountList.isEmpty()) return 0f;
        float sumDayCount = 0f;
        int day = TimeUtil.getDayOfYear(accountList.get(0).getTime());
        for (AccountModel model : accountList) {
            int dayTemp = TimeUtil.getDayOfYear(model.getTime());
            if (dayTemp != day) {
                listFloat.add(sumDayCount);
                day = dayTemp;
                sumDayCount = 0f;
            }
            sumDayCount += model.getCount();
        }
        listFloat.add(sumDayCount);
        return Collections.max(listFloat);
    }
}