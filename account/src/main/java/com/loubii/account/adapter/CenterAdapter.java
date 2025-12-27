package com.loubii.account.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.loubii.account.R;
import com.loubii.account.constants.CenterRes;
import com.loubii.account.util.DensityUtil;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 统一管理：仅负责界面渲染，点击事件全部回调给 Fragment
 */
public class CenterAdapter extends BaseRecycleAdapter {
    private static final int TYPE_END = 1;
    private static final int TYPE_ITEM = 0;
    private Context context;

    // 定义专属于 CenterAdapter 的接口，防止与基类混淆
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public CenterAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            return new ItemHoleder(LayoutInflater.from(context).inflate(R.layout.item_center, parent, false));
        } else if (viewType == TYPE_END) {
            TextView textView = new TextView(context);
            textView.setText("退出登录");
            textView.setTextColor(context.getResources().getColor(R.color.colorTextRed));
            textView.setTextSize(15);
            textView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, DensityUtil.dip2px(30), 0, 0);
            textView.setLayoutParams(layoutParams);
            return new EndHoleder(textView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        if (getItemViewType(position) == TYPE_ITEM) {
            ItemHoleder itemHoleder = (ItemHoleder) holder;
            itemHoleder.mTvCenter.setText(CenterRes.NAMES[position]); //
            itemHoleder.mIvCenter.setImageResource(CenterRes.ICONS[position]); //

            itemHoleder.itemView.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return CenterRes.NAMES.length + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == CenterRes.NAMES.length) ? TYPE_END : TYPE_ITEM;
    }

    class ItemHoleder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_center) ImageView mIvCenter;
        @BindView(R.id.tv_center) TextView mTvCenter;
        public ItemHoleder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class EndHoleder extends RecyclerView.ViewHolder {
        public EndHoleder(View itemView) { super(itemView); }
    }
}