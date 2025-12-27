package com.loubii.account.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;

/**
 * 账单实体类
 * 已经添加了 account 字段用于存储结算账户（如微信、支付宝）
 * @author luo
 * @date 2017/10/23
 */
@Entity
public class AccountModel implements Comparable<AccountModel>{

    @Id(autoincrement = true)
    private Long id; // 用户唯一标识

    private float count; // 记账金额

    private int outIntype; // 支出/收入 1：支出 2：收入

    private String account; // 新增：账户信息（如支付宝、微信、花呗）

    private String detailType; // 具体分类类型（如餐饮、出行）

    private int picRes; // 类型图标资源 ID

    private Date time; // 记账日期

    private String note; // 备注

    private String remark; // 标签/补充说明

    // --- 以下是自动生成或需要重置的部分 ---

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDetailType() {
        return this.detailType;
    }

    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    public int getOutIntype() {
        return this.outIntype;
    }

    public void setOutIntype(int outIntype) {
        this.outIntype = outIntype;
    }

    public float getCount() {
        return this.count;
    }

    public void setCount(float count) {
        this.count = count;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Date getTime() {
        return this.time;
    }

    public int getPicRes() {
        return this.picRes;
    }

    public void setPicRes(int picRes) {
        this.picRes = picRes;
    }

    public AccountModel() {
    }

    @Generated(hash = 434412237)
    public AccountModel(Long id, float count, int outIntype, String account, String detailType,
            int picRes, Date time, String note, String remark) {
        this.id = id;
        this.count = count;
        this.outIntype = outIntype;
        this.account = account;
        this.detailType = detailType;
        this.picRes = picRes;
        this.time = time;
        this.note = note;
        this.remark = remark;
    }

    @Override
    public int compareTo(AccountModel o) {
        if (this.count < o.count)
            return -1;
        else if (this.count > o.count)
            return 1;
        else
            return 0;
    }
}