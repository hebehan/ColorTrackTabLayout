package me.weyye.library.colortrackview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import static android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING;
import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;
import static android.support.v4.view.ViewPager.SCROLL_STATE_SETTLING;

/**
 * Created by Administrator on 2017/4/14 0014.
 */

public class ColorTrackTabLayout extends TabLayout {
    private int mTabTextSize;
    private int mTabSelectedTextColor;
    private int mTabTextColor;
    private ColorTrackTabLayoutOnPageChangeListener mPageChangeListenter;


    public ColorTrackTabLayout(Context context) {
        this(context, null);
    }

    public ColorTrackTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorTrackTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.TabLayout,
                    defStyleAttr, android.support.design.R.style.Widget_Design_TabLayout);
            try {
                int tabTextAppearance = a.getResourceId(android.support.design.R.styleable.TabLayout_tabTextAppearance,
                        android.support.design.R.style.TextAppearance_Design_Tab);

                // Text colors/sizes come from the text appearance first
                final TypedArray ta = context.obtainStyledAttributes(tabTextAppearance,
                        android.support.v7.appcompat.R.styleable.TextAppearance);
                try {
                    //Tab字体大小
                    mTabTextSize = ta.getDimensionPixelSize(
                            android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize, 0);
                    //Tab文字颜色
                    mTabTextColor = ta.getColor(
                            android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor, 0);
                } finally {
                    ta.recycle();
                }

                //Tab文字选中颜色
                mTabSelectedTextColor = a.getColor(android.support.design.R.styleable.TabLayout_tabSelectedTextColor, Color.BLACK);

            } finally {
                a.recycle();
            }
        }
    }

    @Override
    public void addTab(@NonNull Tab tab, int position, boolean setSelected) {
        ColorTrackView colorTrackView = new ColorTrackView(getContext());
        colorTrackView.setProgress(setSelected ? 1 : 0);
        colorTrackView.setText(tab.getText() + "");
        colorTrackView.setTextSize(mTabTextSize);
        colorTrackView.setTag(position);
        colorTrackView.setTextChangeColor(mTabSelectedTextColor);
        colorTrackView.setTextOriginColor(mTabTextColor);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        colorTrackView.setLayoutParams(layoutParams);
        tab.setCustomView(colorTrackView);

        super.addTab(tab, position, setSelected);
        if (position == 0) {
            //默认选中第一个
            setSelectedView(position);
        }

        measureTabWidth(position, colorTrackView);
    }

    private void measureTabWidth(int position, ColorTrackView colorTrackView) {
        ViewGroup slidingTabStrip = (ViewGroup) getChildAt(0);
        ViewGroup tabView = (ViewGroup) slidingTabStrip.getChildAt(position);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

        int w = MeasureSpec.makeMeasureSpec(0,
                MeasureSpec.UNSPECIFIED);
        int h = MeasureSpec.makeMeasureSpec(0,
                MeasureSpec.UNSPECIFIED);
        //手动测量一下
        colorTrackView.measure(w, h);
        params.width = colorTrackView.getMeasuredWidth() + tabView.getPaddingLeft() + tabView.getPaddingRight();
        //设置tabView的宽度
        tabView.setLayoutParams(params);
    }

    /**
     * 设置每个Tab的左内边距和右内边距
     *
     * @param left
     * @param right
     */
    public void setTabPaddingLeftAndRight(int left, int right) {
        try {
            Field mTabPaddingStartField = TabLayout.class.getDeclaredField("mTabPaddingStart");
            Field mTabPaddingEndField = TabLayout.class.getDeclaredField("mTabPaddingEnd");

            mTabPaddingStartField.setAccessible(true);
            mTabPaddingEndField.setAccessible(true);

            mTabPaddingStartField.set(this, left);
            mTabPaddingEndField.set(this, right);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setupWithViewPager(@Nullable ViewPager viewPager, boolean autoRefresh) {
        super.setupWithViewPager(viewPager, autoRefresh);
        try {
            //通过反射找到mPageChangeListener
            Field field = TabLayout.class.getDeclaredField("mPageChangeListener");
            field.setAccessible(true);
            TabLayoutOnPageChangeListener listener = (TabLayoutOnPageChangeListener) field.get(this);
            if (listener != null) {
                //删除自带监听
                viewPager.removeOnPageChangeListener(listener);
                mPageChangeListenter = new ColorTrackTabLayoutOnPageChangeListener(this);
                mPageChangeListenter.reset();
                viewPager.addOnPageChangeListener(mPageChangeListenter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void tabScrolled(int position, float positionOffset) {

        if (positionOffset == 0.0F) {
            return;
        }
        ColorTrackView currentTrackView = getColorTrackView(position);
        ColorTrackView nextTrackView = getColorTrackView(position + 1);
        currentTrackView.setDirection(1);
        currentTrackView.setProgress(1.0F - positionOffset);
        nextTrackView.setDirection(0);
        nextTrackView.setProgress(positionOffset);
    }

    public ColorTrackView getColorTrackView(int position) {
        return (ColorTrackView) getTabAt(position).getCustomView();
    }

    public static class ColorTrackTabLayoutOnPageChangeListener extends TabLayoutOnPageChangeListener {

        private final WeakReference<ColorTrackTabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public ColorTrackTabLayoutOnPageChangeListener(TabLayout tabLayout) {
            super(tabLayout);
            mTabLayoutRef = new WeakReference<>((ColorTrackTabLayout) tabLayout);
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            mPreviousScrollState = mScrollState;
            mScrollState = state;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            ColorTrackTabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout == null) return;
            final boolean updateText = mScrollState != SCROLL_STATE_SETTLING ||
                    mPreviousScrollState == SCROLL_STATE_DRAGGING;
            boolean b = mScrollState != SCROLL_STATE_SETTLING;
            boolean b1 = mPreviousScrollState == SCROLL_STATE_DRAGGING;
            Log.i("tag", "onPageScrolled:" + ",updateText:" + updateText + ",mScrollState:" + b + ",mPreviousScrollState:" + b1);
            if (updateText) {
                tabLayout.tabScrolled(position, positionOffset);
            }
        }

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            ColorTrackTabLayout tabLayout = mTabLayoutRef.get();
            mPreviousScrollState=SCROLL_STATE_SETTLING;
            tabLayout.setSelectedView(position);
        }

        void reset() {
            mPreviousScrollState = mScrollState = SCROLL_STATE_IDLE;
        }

    }


    protected void setSelectedView(int position) {
        final int tabCount = getTabCount();
        if (position < tabCount) {
            for (int i = 0; i < tabCount; i++) {
                getColorTrackView(i).setProgress(i == position ? 1 : 0);
            }
        }
    }
}