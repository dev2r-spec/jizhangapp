package com.loubii.account.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;

import com.loubii.account.R;
import com.loubii.account.adapter.CenterAdapter;
import com.loubii.account.bean.AccountModel;
import com.loubii.account.constants.ClassifyExpendRes;
import com.loubii.account.constants.ClassifyIncomeRes;
import com.loubii.account.db.database.DbHelper;
import com.loubii.account.event.SortEvent;
import com.loubii.account.ui.avtivity.BalanceActivity;
import com.loubii.account.ui.avtivity.BudgetActivity;
import com.loubii.account.ui.avtivity.RemindManagerActivity;
import com.loubii.account.util.ToastUtil;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.PermissionListener;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;

public class FragmentCenter extends BaseFragment {
    @BindView(R.id.rv_mine) RecyclerView mRvMine;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout mCollapsingToolbar;
    @BindView(R.id.appbar) AppBarLayout mAppBarLayout;

    private static final int REQ_CODE = 9999;

    @Override protected void initData() {}

    @Override protected void initView(View view) {
        mAppBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
                mCollapsingToolbar.setTitle("我的");
            } else {
                mCollapsingToolbar.setTitle("");
            }
        });
        initRecycleView();
    }

    private void initRecycleView() {
        mRvMine.setLayoutManager(new LinearLayoutManager(context));
        CenterAdapter adapter = new CenterAdapter(context);
        adapter.setOnItemClickListener(new CenterAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                handleMenuClick(position);
            }
        });
        mRvMine.setAdapter(adapter);
    }

    private void handleMenuClick(int position) {
        switch (position) {
            case 0: startActivity(new Intent(context, BalanceActivity.class)); break;
            case 3: startActivity(new Intent(context, RemindManagerActivity.class)); break;
            case 4: startActivity(new Intent(context, BudgetActivity.class)); break;
            case 5: checkStorage(true); break;
            case 6: checkStorage(false); break;
        }
    }

    private void checkStorage(boolean isExport) {
        AndPermission.with(this).permission(Permission.STORAGE).callback(new PermissionListener() {
            @Override public void onSucceed(int requestCode, List<String> grantedPermissions) {
                if (isExport) performExport(); else openPicker();
            }
            @Override public void onFailed(int requestCode, List<String> deniedPermissions) {
                ToastUtil.showShort(context, "存储权限不足");
            }
        }).start();
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "选择账单 CSV 备份"), REQ_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK && data != null) {
            handleImport(data.getData());
        }
    }

    private void performExport() {
        try {
            List<AccountModel> list = DbHelper.getInstance().author().loadAll();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Account_Backup.csv");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            bw.write("金额,分类,账户,收支类型,日期,备注\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA);
            for (AccountModel m : list) {
                String dStr = (m.getTime() != null) ? sdf.format(m.getTime()) : "";
                bw.write(m.getCount() + "," + m.getDetailType() + "," + (m.getAccount()==null?"默认":m.getAccount()) + ","
                        + (m.getOutIntype()==1?"支出":"收入") + "," + dStr + "," + (m.getNote()==null?"":m.getNote()) + "\n");
            }
            bw.close();
            ToastUtil.showShort(context, "已导出至下载目录");
        } catch (Exception e) { ToastUtil.showShort(context, "导出失败"); }
    }

    private void handleImport(Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            br.readLine();

            ArrayList<AccountModel> list = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (TextUtils.isEmpty(line)) continue;
                String[] d = line.split(",");
                if (d.length < 2) continue;

                AccountModel m = new AccountModel();
                m.setId(null);
                m.setCount(Float.parseFloat(d[0].trim()));
                String category = d[1].trim();
                m.setDetailType(category);
                m.setAccount(d.length > 2 ? d[2].trim() : "默认");

                // 1支出，2收入
                int type = (d.length > 3 && d[3].contains("收入")) ? 2 : 1;
                m.setOutIntype(type);

                // 【图标精准匹配】
                m.setPicRes(getIconResId(category, type));

                m.setTime(smartParseDate(d.length > 4 ? d[4].trim() : ""));
                m.setNote(d.length > 5 ? d[5].trim() : "");
                m.setRemark("");
                list.add(m);
            }
            br.close();

            if (!list.isEmpty()) {
                DbHelper.getInstance().author().deleteAll();
                DbHelper.getInstance().author().insertInTx(list);
                EventBus.getDefault().post(new SortEvent("OK"));
                ToastUtil.showShort(context, "恢复成功 " + list.size() + " 条，图标已同步");
            }
        } catch (Exception e) { ToastUtil.showShort(context, "导入解析失败"); }
    }

    /**
     * 【图标智能识别】
     * 遍历收入/支出资源类中的 NAMES 数组，找到对应图标索引
     */
    private int getIconResId(String categoryName, int type) {
        if (type == 1) { // 支出
            for (int i = 0; i < ClassifyExpendRes.NAMES.length; i++) {
                if (categoryName.equals(ClassifyExpendRes.NAMES[i])) {
                    return ClassifyExpendRes.ICONS[i];
                }
            }
            return R.drawable.classify_eat; // 支出保底
        } else { // 收入
            for (int i = 0; i < ClassifyIncomeRes.NAMES.length; i++) {
                if (categoryName.equals(ClassifyIncomeRes.NAMES[i])) {
                    return ClassifyIncomeRes.ICONS[i];
                }
            }
            return R.drawable.classify_income_wage; // 收入保底
        }
    }

    private Date smartParseDate(String input) {
        if (TextUtils.isEmpty(input)) return new Date();
        input = input.replace("\"", "").trim();
        String[] patterns = {"yyyy/MM/dd HH:mm", "yyyy/MM/dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        for (String p : patterns) {
            try { return new SimpleDateFormat(p, Locale.CHINA).parse(input); } catch (Exception e) { continue; }
        }
        return new Date();
    }

    @Override protected int getLayoutId() { return R.layout.fragment_center; }
}