package com.loubii.account.ui.fragments.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.beiing.leafchart.SlideSelectLineChart;
import com.beiing.leafchart.bean.Axis;
import com.beiing.leafchart.bean.AxisValue;
import com.beiing.leafchart.bean.Line;
import com.beiing.leafchart.bean.PointValue;
import com.loubii.account.R;
import com.loubii.account.bean.AccountModel;
import com.loubii.account.constants.ChartConfig;
import com.loubii.account.constants.Extra;
import com.loubii.account.db.AccountModelDao;
import com.loubii.account.db.database.DbHelper;
import com.loubii.account.event.ChartClassifyEvent;
import com.loubii.account.event.ChartYearEvent;
import com.loubii.account.ui.fragments.BaseEventFragment;
import com.loubii.account.ui.fragments.ChartTypeFragment;
import com.loubii.account.ui.fragments.FragmentChart;
import com.loubii.account.util.AccListUtil;
import com.loubii.account.util.NumUtil;
import com.loubii.account.util.TimeUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.BindView;

public class ChartDetailFragment extends BaseEventFragment {

    private static final String TIME_TYPE = "TIME_TYPE";
    private static final String END_TIME = "END_TIME";
    private static final String MAX_VALUE = "MAX_VALUE";
    private static final String SELECT_YEAR = "SELECT_YEAR";

    @BindView(R.id.select_chart) SlideSelectLineChart mSelectChart;
    @BindView(R.id.rv_chart_classify) RecyclerView mRvChartClassify;
    @BindView(R.id.app_bar) AppBarLayout mAppBar;
    @BindView(R.id.tv_expend_total_des) TextView mTvExpendTotalDes;
    @BindView(R.id.tv_expend_total) TextView mTvExpendTotal;
    @BindView(R.id.tv_account_total) TextView mTvAccountTotal;
    @BindView(R.id.tv_account_max) TextView mTvAccountMax;

    private int mTimeType;
    private int mTime; // 月视图下代表月份(1-12)
    private int mSelectedYear;
    private int mDays;
    private List<AccountModel> mAccountList = new ArrayList<>();
    private Date mDateStart, mDateEnd;
    private float mMaxValue = 0f;
    private ArrayList<Float> mFloatList;
    private int mAccountType = Extra.ACCOUNT_TYPE_EXPEND;
    private int mSelectPosition = 0;
    private List<ChartDataBean> mRecycleList = new ArrayList<>();
    private ChartDetailCountAdapter mAdapter;
    private String mDetailType = Extra.DETAIL_TYPE_DEFAULT;
    private float mDaySumCount;

    public static ChartDetailFragment newInstance(int timeType, int endTime, float maxValue, int year) {
        ChartDetailFragment fragment = new ChartDetailFragment();
        Bundle args = new Bundle();
        args.putInt(TIME_TYPE, timeType);
        args.putInt(END_TIME, endTime);
        args.putFloat(MAX_VALUE, maxValue);
        args.putInt(SELECT_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTimeType = getArguments().getInt(TIME_TYPE);
            mTime = getArguments().getInt(END_TIME);
            mMaxValue = getArguments().getFloat(MAX_VALUE);
            mSelectedYear = getArguments().getInt(SELECT_YEAR, Calendar.getInstance().get(Calendar.YEAR));
        }
    }

    @Override
    protected int getLayoutId() { return R.layout.fragment_chart_detail; }

    @Override
    protected void initData() {
        if (getParentFragment() != null && getParentFragment().getParentFragment() instanceof FragmentChart) {
            FragmentChart fragmentChart = (FragmentChart) (getParentFragment().getParentFragment());
            mDetailType = fragmentChart.getDetailType();
        }
        refreshAllData();
    }

    private void refreshAllData() {
        initChartData();
        setDaySumCount();
        setRecycleListData(mSelectPosition);
    }

    private void setDaySumCount() {
        List<AccountModel> accountList = getAccountModels(mSelectPosition, Extra.DETAIL_TYPE_DEFAULT);
        mDaySumCount = AccListUtil.sum(accountList);
    }

    @Override
    protected void initView(View view) {
        initTitleText();
        initLineChart();
        initRecycleView();
        initListener();
    }

    private void initTitleText() {
        String yearPrefix = mSelectedYear + "年";
        switch (mTimeType) {
            case ChartTypeFragment.TYPE_WEEK:
                mTvExpendTotalDes.setText(yearPrefix + "第" + mTime + "周支出");
                break;
            case ChartTypeFragment.TYPE_MONTH:
                mTvExpendTotalDes.setText(yearPrefix + mTime + "月支出");
                break;
            case ChartTypeFragment.TYPE_YEAR:
                mTvExpendTotalDes.setText(yearPrefix + "年度支出");
                break;
        }

        if (mAccountList != null && mAccountList.size() > 0) {
            mTvExpendTotal.setText(String.valueOf(AccListUtil.sum(mAccountList)));
            mTvAccountTotal.setText(String.valueOf(mAccountList.size()));
            mTvAccountMax.setText(String.valueOf(AccListUtil.max(mAccountList)));
        } else {
            mTvExpendTotal.setText("0.0");
            mTvAccountTotal.setText("0");
            mTvAccountMax.setText("0.0");
        }
    }

    private void initListener() {
        mSelectChart.setOnPointSelectListener((position, xLabel, value) -> {
            mSelectPosition = position;
            setDaySumCount();
            setRecycleListData(position);
            mAdapter.notifyDataSetChanged();
        });
    }

    private void initRecycleView() {
        mRvChartClassify.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new ChartDetailCountAdapter(context, mRecycleList);
        mRvChartClassify.setAdapter(mAdapter);
    }

    /**
     * 【关键修复】精准计算每个页面的起始日期，解决“11月显示12月坐标”的问题
     */
    private void initChartData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (mTimeType) {
            case ChartTypeFragment.TYPE_WEEK:
                mDays = 7;
                mDateStart = TimeUtil.getFirstDayOfWeek(mSelectedYear, mTime);
                mDateEnd = TimeUtil.getEndDayOfWeek(mSelectedYear, mTime);
                break;
            case ChartTypeFragment.TYPE_MONTH:
                // 核心修复：基于传入的年份和月份(mTime)强行重置日期
                cal.set(Calendar.YEAR, mSelectedYear);
                cal.set(Calendar.MONTH, mTime - 1); // Calendar标准 0-11
                cal.set(Calendar.DAY_OF_MONTH, 1);
                mDateStart = cal.getTime();

                mDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                cal.set(Calendar.DAY_OF_MONTH, mDays);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                mDateEnd = cal.getTime();
                break;
            case ChartTypeFragment.TYPE_YEAR:
                mDays = 12;
                mDateStart = TimeUtil.getFirstDayOfYear(mSelectedYear);
                mDateEnd = TimeUtil.getEndDayOfYear(mSelectedYear);
                break;
        }
        mAccountList = getAccountList(mAccountType, mDetailType, mDateStart, mDateEnd);
        mFloatList = getValues(mAccountList);
    }

    /**
     * 【修复】数据点对齐逻辑
     */
    private ArrayList<Float> getValues(List<AccountModel> accountList) {
        ArrayList<Float> list = new ArrayList<>();
        Calendar dataCal = Calendar.getInstance();
        for (int i = 1; i <= mDays; i++) {
            int targetValue = (mTimeType == ChartTypeFragment.TYPE_YEAR) ? (i - 1) : i;
            float sumDayCount = 0f;
            if (accountList != null) {
                for (AccountModel model : accountList) {
                    dataCal.setTime(model.getTime());
                    int currentValue = (mTimeType == ChartTypeFragment.TYPE_YEAR)
                            ? dataCal.get(Calendar.MONTH)
                            : dataCal.get(Calendar.DAY_OF_MONTH);
                    if (targetValue == currentValue) sumDayCount += model.getCount();
                }
            }
            list.add(sumDayCount);
        }
        return list;
    }

    private void initLineChart() {
        Axis axisX = new Axis(getAxisValuesX());
        axisX.setAxisColor(Color.parseColor("#a9a6b8")).setTextColor(Color.parseColor("#a9a6b8")).setHasLines(false).setShowText(true);
        Axis axisY = new Axis(getAxisValuesY());
        axisY.setAxisColor(Color.TRANSPARENT).setTextColor(Color.DKGRAY).setHasLines(true).setShowText(false);

        mSelectChart.setAxisX(axisX);
        mSelectChart.setAxisY(axisY);
        mSelectChart.setSlideLine(ChartConfig.getSlideingLine());
        mSelectChart.setChartData(getFoldLine());
        mSelectChart.setSelectedPoint(mSelectPosition);
        mSelectChart.show();
    }

    /**
     * 【修复】X轴日期标签生成逻辑，确保11月显示11-01起始
     */
    private List<AxisValue> getAxisValuesX() {
        List<AxisValue> axisValues = new ArrayList<>();
        for (int i = 1; i <= mDays; i++) {
            AxisValue value = new AxisValue();
            if (mTimeType == ChartTypeFragment.TYPE_YEAR) {
                value.setLabel(i + "月");
                value.setShowLabel(true);
            } else {
                // 核心修复：基于该月起始日期 mDateStart 递增生成标签
                Date currentDate = TimeUtil.getDistanceDate(mDateStart, i - 1);
                value.setLabel(TimeUtil.date2String(currentDate, "MM-dd"));
                value.setShowLabel(mTimeType != ChartTypeFragment.TYPE_MONTH || i % 5 == 0);
            }
            axisValues.add(value);
        }
        return axisValues;
    }

    private List<AxisValue> getAxisValuesY() {
        List<AxisValue> axisValues = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            AxisValue value = new AxisValue();
            value.setLabel(String.valueOf(i * (mMaxValue / 10)));
            axisValues.add(value);
        }
        return axisValues;
    }

    private Line getFoldLine() {
        List<PointValue> pointValues = new ArrayList<>();
        for (int i = 1; i <= mDays; i++) {
            PointValue pointValue = new PointValue();
            pointValue.setX((i - 1) / (mDays - 1f));
            float val = mFloatList.get(i - 1);
            pointValue.setLabel(String.valueOf(val));
            pointValue.setY(mMaxValue == 0 ? 0f : val / mMaxValue);
            pointValues.add(pointValue);
        }
        return ChartConfig.getLine(pointValues);
    }

    private void setRecycleListData(int position) {
        mRecycleList.clear();
        List<AccountModel> accountList = getAccountModels(position, mDetailType);
        if (accountList != null && accountList.size() > 0) {
            String type = accountList.get(0).getDetailType();
            int imgRes = accountList.get(0).getPicRes();
            float sumAccountClassify = 0f;
            int addCount = 0;
            for (AccountModel model : accountList) {
                if (!model.getDetailType().equals(type)) {
                    mRecycleList.add(getChartDataBean(type, imgRes, sumAccountClassify, addCount));
                    sumAccountClassify = 0f;
                    addCount = 0;
                    type = model.getDetailType();
                    imgRes = model.getPicRes();
                }
                sumAccountClassify += model.getCount();
                addCount++;
            }
            mRecycleList.add(getChartDataBean(type, imgRes, sumAccountClassify, addCount));
        }
        Collections.sort(mRecycleList);
    }

    private ChartDataBean getChartDataBean(String type, int imgRes, float sumAccountClassify, int addCount) {
        ChartDataBean chartBean = new ChartDataBean();
        chartBean.setTotal(sumAccountClassify);
        chartBean.setCount(addCount);
        chartBean.setName(type);
        chartBean.setImgRes(imgRes);
        chartBean.setPrecent(mDaySumCount == 0 ? 0 : NumUtil.getPointFloat(sumAccountClassify / mDaySumCount, 4));
        return chartBean;
    }

    private List<AccountModel> getAccountModels(int position, String detailType) {
        if (mTimeType == ChartTypeFragment.TYPE_YEAR) {
            Date selectedMonth = TimeUtil.getFirstDayOfMonth(mSelectedYear, position + 1);
            return getAccountList(mAccountType, detailType, selectedMonth, TimeUtil.getEndDayOfMonth(mSelectedYear, position + 1));
        } else {
            Date selectedDate = TimeUtil.getDistanceDate(mDateStart, position);
            return getAccountList(mAccountType, detailType, TimeUtil.getDayStartTime(selectedDate), TimeUtil.getDayEndTime(selectedDate));
        }
    }

    public List<AccountModel> getAccountList(int accountType, String detailType, Date startTime, Date endTime) {
        QueryBuilder<AccountModel> builder = DbHelper.getInstance().author().queryBuilder()
                .where(AccountModelDao.Properties.Time.between(startTime, endTime),
                        AccountModelDao.Properties.OutIntype.eq(accountType));
        if (!detailType.equals(Extra.DETAIL_TYPE_DEFAULT)) {
            builder.where(AccountModelDao.Properties.DetailType.eq(detailType));
        }
        builder.orderAsc(AccountModelDao.Properties.DetailType);
        return builder.list();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onYearEvent(ChartYearEvent event) {
        this.mSelectedYear = event.year;
        refreshAllData();
        initTitleText();
        initLineChart();
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ChartClassifyEvent classifyEvent) {
        mDetailType = classifyEvent.getMessage();
        refreshAllData();
        initLineChart();
        mAdapter.notifyDataSetChanged();
    }
}