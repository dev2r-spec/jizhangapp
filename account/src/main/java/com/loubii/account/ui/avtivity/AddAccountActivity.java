package com.loubii.account.ui.avtivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bigkoo.pickerview.OptionsPickerView;
import com.loubii.account.R;
import com.loubii.account.adapter.GridPagerAdapter;
import com.loubii.account.adapter.ViewPagerClassifyAdapter;
import com.loubii.account.bean.AccountModel;
import com.loubii.account.bean.RecycleClassifyPagerBean;
import com.loubii.account.constants.ClassifyExpendRes;
import com.loubii.account.constants.ClassifyIncomeRes;
import com.loubii.account.constants.Extra;
import com.loubii.account.db.database.DBManager;
import com.loubii.account.db.database.DbHelper;
import com.loubii.account.event.SortEvent;
import com.loubii.account.ui.dialog.AddRemarkDialog;
import com.loubii.account.util.KeyboardUtil;
import com.loubii.account.util.SPUtil;
import com.loubii.account.util.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;

public class AddAccountActivity extends BaseActivity {

    private static final int PAGE_SIZE = 8;
    private static final boolean INIT = false;
    private static final boolean CHANGE = true;

    @BindView(R.id.vp_classify) ViewPager mVpClassify;
    @BindView(R.id.et_count) EditText mEtCount;
    @BindView(R.id.tv_calendar) TextView mTvCalendar;
    @BindView(R.id.tv_remark) TextView mTvRemark;
    @BindView(R.id.tv_account) TextView mTvAccount;
    @BindView(R.id.indicator) CircleIndicator mIndicator;
    @BindView(R.id.ll_title_contract) FrameLayout mLlTitleContract;
    @BindView(R.id.rg_type) RadioGroup mRgType;
    @BindView(R.id.iv_classify) ImageView mIvClassify;

    private List<RecycleClassifyPagerBean> mExpendCommonList = new ArrayList<>();
    private List<RecycleClassifyPagerBean> mIncomeList = new ArrayList<>();
    private List<View> mPagerList = new ArrayList<>();
    private ArrayList<GridPagerAdapter> mAdapterList = new ArrayList<>();
    private ArrayList<String> mAccountOptions = new ArrayList<>();

    private KeyboardUtil mKeyboardUtil;
    private OptionsPickerView mAccountPicker;

    private int mOutInType = 1;
    private int mCurrentSelectPosition = 0;
    private String mStrAccount = "支付宝";
    private Date mSelectTime;
    private DBManager<AccountModel, Long> mDbManager;
    private String mStrNote;

    @Override
    protected int getLayoutId() { return R.layout.activity_add_account; }

    @Override
    protected void initData() {
        mDbManager = DbHelper.getInstance().author();
        refreshAccountList();
    }

    private void refreshAccountList() {
        mAccountOptions.clear();
        mAccountOptions.add("支付宝");
        mAccountOptions.add("微信");
        mAccountOptions.add("现金");
        mAccountOptions.add("银行卡");

        List<AccountModel> allRecords = mDbManager.loadAll();
        if (allRecords != null) {
            for (AccountModel model : allRecords) {
                String name = model.getAccount();
                if (!TextUtils.isEmpty(name) && !mAccountOptions.contains(name)) {
                    mAccountOptions.add(name);
                }
            }
        }
        mAccountOptions.add("+ 新增自定义账户");
    }

    @Override
    protected void initView() {
        mLlTitleContract.setVisibility(View.VISIBLE);
        mTvAccount.setText(mStrAccount);
        mSelectTime = new Date();
        mTvCalendar.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(mSelectTime));

        setListener();
        setRecycleData(Extra.ACCOUNT_TYPE_EXPEND);
        initPagerWithGridView(INIT);
        initInteraction();
        EventBus.getDefault().register(this);
    }

    private void setRecycleData(int type) {
        if (type == Extra.ACCOUNT_TYPE_EXPEND) {
            mExpendCommonList = SPUtil.getCommonList(this, type);
            if (mExpendCommonList.isEmpty()) {
                for (int i = 0; i < ClassifyExpendRes.NAMES.length; i++) {
                    RecycleClassifyPagerBean bean = new RecycleClassifyPagerBean();
                    bean.setName(ClassifyExpendRes.NAMES[i]);
                    bean.setIconRes(ClassifyExpendRes.ICONS[i]);
                    mExpendCommonList.add(bean);
                }
            }
            // 【修复】将“设置”入口图标设为现有的餐饮图标，解决资源找不到报错
            RecycleClassifyPagerBean setBean = new RecycleClassifyPagerBean();
            setBean.setName("设置");
            setBean.setIconRes(R.drawable.classify_eat);
            mExpendCommonList.add(setBean);
            mIvClassify.setImageResource(mExpendCommonList.get(0).getIconRes());
        } else {
            mIncomeList = SPUtil.getCommonList(this, type);
            if (mIncomeList.isEmpty()) {
                for (int i = 0; i < ClassifyIncomeRes.NAMES.length; i++) {
                    RecycleClassifyPagerBean bean = new RecycleClassifyPagerBean();
                    bean.setName(ClassifyIncomeRes.NAMES[i]);
                    bean.setIconRes(ClassifyIncomeRes.ICONS[i]);
                    mIncomeList.add(bean);
                }
            }
            RecycleClassifyPagerBean setBean = new RecycleClassifyPagerBean();
            setBean.setName("设置");
            setBean.setIconRes(R.drawable.classify_eat);
            mIncomeList.add(setBean);
            mIvClassify.setImageResource(mIncomeList.get(0).getIconRes());
        }
    }

    private void initPagerWithGridView(boolean hasChange) {
        final List<RecycleClassifyPagerBean> gridList = (mOutInType == 1) ? mExpendCommonList : mIncomeList;
        mPagerList.clear();
        mAdapterList.clear();
        int pageCount = (int) Math.ceil(gridList.size() * 1.0 / PAGE_SIZE);
        for (int i = 0; i < pageCount; i++) {
            GridView gridView = (GridView) LayoutInflater.from(this).inflate(R.layout.gridview_pager_classify, null);
            // 【修复】使用局部变量 gridAdapter 供点击事件内部调用
            final GridPagerAdapter gridAdapter = new GridPagerAdapter(this, gridList, i, PAGE_SIZE);
            gridView.setAdapter(gridAdapter);
            mAdapterList.add(gridAdapter);
            mPagerList.add(gridView);

            final int page = i;
            gridView.setOnItemClickListener((parent, view, position, id) -> {
                int realPos = position + page * PAGE_SIZE;
                if (gridList.get(realPos).getName().equals("设置")) {
                    // 【修复】跳转至清单对应的 AddSortActivity 处理分类自定义
                    startActivity(new Intent(AddAccountActivity.this, AddSortActivity.class));
                } else {
                    mCurrentSelectPosition = realPos;
                    mIvClassify.setImageResource(gridList.get(mCurrentSelectPosition).getIconRes());
                    gridAdapter.onItemSelected(position);
                }
            });
        }
        mVpClassify.setAdapter(new ViewPagerClassifyAdapter(mPagerList));
        mIndicator.setViewPager(mVpClassify);
    }

    private void initInteraction() {
        mKeyboardUtil = new KeyboardUtil(this, this, mEtCount);
        mEtCount.setInputType(InputType.TYPE_NULL);
        mEtCount.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mKeyboardUtil.showKeyboard();
                return true;
            }
            return false;
        });

        mTvCalendar.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mKeyboardUtil != null) mKeyboardUtil.hideKeyboard();
                showNativeDatePicker();
                return true;
            }
            return false;
        });

        mKeyboardUtil.setOnKeyListener(result -> {
            saveData(result);
            new Handler().postDelayed(this::finish, 150);
        });
    }

    private void showNativeDatePicker() {
        final Calendar c = Calendar.getInstance();
        c.setTime(mSelectTime != null ? mSelectTime : new Date());
        new DatePickerDialog(this, (view, y, m, d) -> {
            c.set(y, m, d);
            mSelectTime = c.getTime();
            mTvCalendar.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(mSelectTime));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showAccountPicker() {
        refreshAccountList();
        mAccountPicker = new OptionsPickerView.Builder(this, (o1, o2, o3, v) -> {
            String selected = mAccountOptions.get(o1);
            if (selected.equals("+ 新增自定义账户")) {
                showAddCustomAccountDialog();
            } else {
                mStrAccount = selected;
                mTvAccount.setText(mStrAccount);
            }
        }).setTitleText("选择账户").build();
        mAccountPicker.setPicker(mAccountOptions);
        mAccountPicker.show();
    }

    private void showAddCustomAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新增账户");
        final EditText input = new EditText(this);
        input.setHint("如：饭卡");
        builder.setView(input);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                mStrAccount = name;
                mTvAccount.setText(mStrAccount);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void saveData(float count) {
        if (count <= 0) return;
        AccountModel model = new AccountModel();
        model.setCount(count);
        model.setOutIntype(mOutInType);
        model.setAccount(mStrAccount);
        model.setTime(mSelectTime != null ? mSelectTime : new Date());
        List<RecycleClassifyPagerBean> currentList = (mOutInType == 1) ? mExpendCommonList : mIncomeList;
        if (mCurrentSelectPosition < currentList.size()) {
            model.setDetailType(currentList.get(mCurrentSelectPosition).getName());
            model.setPicRes(currentList.get(mCurrentSelectPosition).getIconRes());
        }
        if (!TextUtils.isEmpty(mStrNote)) model.setNote(mStrNote);
        mDbManager.insert(model);
        EventBus.getDefault().post(new SortEvent("refresh"));
    }

    @OnClick({R.id.tv_remark, R.id.ll_account_root})
    public void onViewClicked(View view) {
        if (mKeyboardUtil != null) mKeyboardUtil.hideKeyboard();
        switch (view.getId()) {
            case R.id.tv_remark: showRemarkDialog(); break;
            case R.id.ll_account_root: showAccountPicker(); break;
        }
    }

    private void setListener() {
        mRgType.setOnCheckedChangeListener((group, checkedId) -> {
            mCurrentSelectPosition = 0;
            mOutInType = (checkedId == R.id.rb_expend) ? 1 : 2;
            setRecycleData(mOutInType == 1 ? Extra.ACCOUNT_TYPE_EXPEND : Extra.ACCOUNT_TYPE_INCOME);
            initPagerWithGridView(INIT);
        });
    }

    private void showRemarkDialog() {
        AddRemarkDialog dialog = new AddRemarkDialog(this);
        dialog.show();
        dialog.setOnFinishListener((remarkStr, list) -> {
            mTvRemark.setText(remarkStr);
            mStrNote = remarkStr;
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SortEvent event) {
        setRecycleData(mOutInType == 1 ? Extra.ACCOUNT_TYPE_EXPEND : Extra.ACCOUNT_TYPE_INCOME);
        initPagerWithGridView(CHANGE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}