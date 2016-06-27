package com.pingshow.amper;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.TextView;

import com.pingshow.amper.view.HackyViewPager;

import java.util.ArrayList;

/**
 * Created by jack on 2016/5/17.
 * 图片查看器
 */
public class ImagePagerActivity extends FragmentActivity{
    private static final String STATE_POSITION = "STATE_POSITION";
    public static final String EXTRA_IMAGE_INDEX = "image_index";
    public static final String EXTRA_IMAGE_URLS = "image_urls";
    public static final String EXTRA_IMAGE_TIMES = "image_times";

    private HackyViewPager mPager;
    private int pagerPosition;
    private TextView indicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_pager);

        pagerPosition = getIntent().getIntExtra(EXTRA_IMAGE_INDEX, 0);
        ArrayList<String> urls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        ArrayList<String> times = getIntent().getStringArrayListExtra(EXTRA_IMAGE_TIMES);

        mPager = (HackyViewPager) findViewById(R.id.pager);
        ImagePagerAdapter mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), urls,times);
        mPager.setAdapter(mAdapter);
        indicator = (TextView) findViewById(R.id.indicator);

        CharSequence text = getString(R.string.viewpager_indicator, 1, mPager.getAdapter().getCount());
        indicator.setText(text);
        // 更新下标
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int arg0) {
                CharSequence text = getString(R.string.viewpager_indicator, arg0 + 1, mPager.getAdapter().getCount());
                indicator.setText(text);
            }

        });
        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt(STATE_POSITION);
        }

        mPager.setCurrentItem(pagerPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_POSITION, mPager.getCurrentItem());
    }

    private class ImagePagerAdapter extends FragmentStatePagerAdapter {

        public ArrayList<String> fileList;
        public ArrayList<String> timeList;

        public ImagePagerAdapter(FragmentManager fm, ArrayList<String> fileList, ArrayList<String> timeList) {
            super(fm);
            this.fileList = fileList;
            this.timeList = timeList;
        }

        @Override
        public int getCount() {
            return fileList == null ? 0 : fileList.size();
        }

        @Override
        public Fragment getItem(int position) {
            String url = fileList.get(position);
            String time ="";
            if (!TextUtils.isEmpty(timeList.get(position))) {
                time = DateUtils.formatDateTime(getApplicationContext(), Long.parseLong(timeList.get(position)),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_WEEKDAY |
                                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_CAP_AMPM);
            }
            return ImageDetailFragment.newInstance(url,time);
        }

    }
}
