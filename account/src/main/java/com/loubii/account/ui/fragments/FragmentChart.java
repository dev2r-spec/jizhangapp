package com.loubii.account.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bigkoo.pickerview.OptionsPickerView;
import com.loubii.account.R;
import com.loubii.account.bean.AccountModel;
import com.loubii.account.constants.Extra;
import com.loubii.account.db.database.DbHelper;
import com.loubii.account.event.ChartClassifyEvent;
import com.loubii.account.event.ChartYearEvent; // 需要新建这个 Event 类
import com.loubii.account.ui.dialog.ListPopWindow;
import com.loubii.account.ui.fragments.adapter.BaseFragmentPagerAdapter;
import com.loubii.account.ui.fragments.chart.ChartPopWindowAdapter;
import com.loubii.account.util.AccListUtil;
import com.loubii.account.util.DensityUtil;
import com.loubii.account.view.SliderLayout;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 图表 Fragment 完整修复版
 * 解决了：年份固定无法切换的问题
 */
public class FragmentChart extends BaseFragment {
    @BindView(R.id.ll_title_return) FrameLayout mLlTitleReturn;
    @BindView(R.id.rb_expend) RadioButton mRbExpend;
    @BindView(R.id.rb_income) RadioButton mRbIncome;
    @BindView(R.id.vp_chart) ViewPager mVpChart;
    @BindView(R.id.tab_year_month) TabLayout mTabYearMonth;
    @BindView(R.id.slider_layout) SliderLayout mSliderLayout;
    @BindView(R.id.tv_classify) TextView mTvClassify;

    // 新增：标题栏年份显示
    @BindView(R.id.tv_chart_year) TextView mTvChartYear;

    private ListPopWindow mListPopWindow;
    private List<AccountModel> mPopData = new ArrayList<>();
    private ArrayList<String> mTitleList = new ArrayList<>();
    private ArrayList<ChartTypeFragment> mFragmentList = new ArrayList<>();

    private int mAccountType = Extra.ACCOUNT_TYPE_EXPEND;
    private String mDetailType = Extra.DETAIL_TYPE_DEFAULT;
    private int mSelectedYear; // 当前选中的年份

    @Override
    protected int getLayoutId() { return R.layout.fragment_chart; }

    @Override
    protected void initData() {
        mSelectedYear = Calendar.getInstance().get(Calendar.YEAR); // 默认今年
        initPopData();
        initChartData();
    }

    private void initChartData() {
        mTitleList.clear();
        mFragmentList.clear();
        mTitleList.add("周");
        mTitleList.add("月");
        mTitleList.add("年");

        // 将选中的年份传递给子 Fragment
        mFragmentList.add(ChartTypeFragment.newInstance(ChartTypeFragment.TYPE_WEEK, mSelectedYear));
        mFragmentList.add(ChartTypeFragment.newInstance(ChartTypeFragment.TYPE_MONTH, mSelectedYear));
        mFragmentList.add(ChartTypeFragment.newInstance(ChartTypeFragment.TYPE_YEAR, mSelectedYear));
    }

    @Override
    protected void initView(View view) {
        mLlTitleReturn.setVisibility(View.GONE);
        mTvChartYear.setText(mSelectedYear + "年"); // 初始化年份显示
        initViewPager();
    }

    /**
     * 【新增】点击年份弹出选择器
     */
    @OnClick(R.id.tv_chart_year)
    public void onYearPickerClick() {
        final ArrayList<String> years = new ArrayList<>();
        for (int i = 2020; i <= 2030; i++) years.add(i + "年");

        OptionsPickerView pvOptions = new OptionsPickerView.Builder(context, (options1, options2, options3, v) -> {
            mSelectedYear = 2020 + options1;
            mTvChartYear.setText(mSelectedYear + "年");
            // 通过 EventBus 通知所有子 Fragment 刷新年份数据
            EventBus.getDefault().post(new ChartYearEvent(mSelectedYear));
        }).setTitleText("选择年份").build();

        pvOptions.setPicker(years);
        pvOptions.setSelectOptions(mSelectedYear - 2020);
        pvOptions.show();
    }

    private void initViewPager() {
        BaseFragmentPagerAdapter adapter = new BaseFragmentPagerAdapter(getChildFragmentManager(), mFragmentList, mTitleList);
        mVpChart.setAdapter(adapter);
        mVpChart.setOffscreenPageLimit(2);
        mVpChart.setCurrentItem(1); // 默认显示“月”
        mTabYearMonth.setupWithViewPager(mVpChart);
        mVpChart.addOnPageChangeListener(new SliderLayout.SliderOnPageChangeListener(mTabYearMonth, mSliderLayout));
    }

    @OnClick(R.id.tv_classify)
    public void onClassifyClick() {
        if (mPopData == null || mPopData.size() < 2) return;
        mListPopWindow = new ListPopWindow(context, mPopData.size());
        mListPopWindow.setAnchorView(mTvClassify);
        mListPopWindow.setAdapter(new ChartPopWindowAdapter(context, mPopData));
        mListPopWindow.setOnItemClickListener((parent, view, position, id) -> {
            mTvClassify.setText(mPopData.get(position).getDetailType());
            mListPopWindow.dismiss();
            String message = (position == 0) ? Extra.DETAIL_TYPE_DEFAULT : mPopData.get(position).getDetailType();
            if (mDetailType.equals(message)) return;
            mDetailType = message;
            EventBus.getDefault().post(new ChartClassifyEvent(message));
        });
        mListPopWindow.show();
    }

    private void initPopData() {
        mPopData.clear();
        addHeaderToPop();
        Date maxDate = DbHelper.getInstance().getMaxDate();
        Date minDate = DbHelper.getInstance().getMinDate();
        if (minDate != null && maxDate != null) {
            List<AccountModel> accountList = DbHelper.getInstance().getAccountList(mAccountType, minDate, maxDate);
            mPopData.addAll(AccListUtil.removeRepeat(accountList));
        }
    }

    private void addHeaderToPop() {
        AccountModel bean = new AccountModel();
        bean.setDetailType("全部");
        bean.setPicRes(R.drawable.classify_eat);
        mPopData.add(bean);
    }
    // 在 FragmentChart 类内部添加这个方法
    public String getDetailType() {
        return mDetailType;
    }
}