package com.pingshow.amper.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pingshow.amper.R;
import com.pingshow.util.MyUtil;

import java.lang.reflect.Field;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by HwH on 2016/1/13.
 */
public class RefreshableView extends LinearLayout implements View.OnTouchListener {
    public static final String TAG = "RefreshableView";
    /**
     * 一分钟的毫秒值,用于判断上次的更新时间
     */
    private static final long ONE_MINUTE = 60 * 1000;
    /**
     * 一小时的毫秒值,用于判断上次更新时间
     */
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    /**
     * 一天的毫秒值,用于判断上次更新时间
     */
    private static final long ONE_DAY = 24 * ONE_HOUR;
    /**
     * 一个月的毫秒值,用于判断上次更新时间
     */
    private static final long ONE_MONTH = 30 * ONE_DAY;
    /**
     * 一年的毫秒值,用于判断上次更新时间
     */
    private static final long ONE_YEAR = 12 * ONE_MONTH;
    /**
     * 下拉头部回滚的速度
     */
    private static final int SCROLL_SPEED = -20;
    private  Context context;
    /**
     * 下拉刷新的回调接口
     */
    private PullToRefreshListener mListener;
    /**
     * 下拉刷新的头
     */
    private View header;
    /**
     * 刷新时显示的进度条
     */
    private ProgressBar progressBar;
    /**
     * 上次更新时间的文字描述
     */
    private TextView updateAt;
    /**
     * 下拉刷新时候的文字描述
     */
    private TextView description;
    /**
     * 下拉刷新的箭头
     */
    private ImageView arrow;
    /**
     * 在被判定为滚动之前用户手指可以移动的最大值。
     */
    private int touchSlop;
    /**
     * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
     */
    private boolean loadOnce;
    /**
     * 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉
     */
    private boolean ableToPull;

    private SharedPreferences preferences;
    /**
     * 上次更新时间的字符串常量，用于作为SharedPreferences的键值
     */
    private static final String UPDATED_AT = "updated_at";
    /**
     * 为了防止不同界面的下拉刷新在上次更新时间上互相有冲突，使用id来做区分
     */
    private int mId = -1;
    /**
     * 当前处理什么状态，可选值有STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
     * STATUS_REFRESHING 和 STATUS_REFRESH_FINISHED
     */
    private int currentStatus = STATUS_REFRESH_FINISHED;
    /**
     * 记录上一次的状态是什么，避免进行重复操作
     */
    private int lastStatus = currentStatus;
    /**
     * 下拉状态
     */
    public static final int STATUS_PULL_TO_REFRESH = 0;
    /**
     * 释放立即刷新状态
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 1;
    /**
     * 正在刷新状态
     */
    public static final int STATUS_REFRESHING = 2;

    /**
     * 刷新完成或未刷新状态
     */
    public static final int STATUS_REFRESH_FINISHED = 3;
    /**
     * 上次更新的毫秒值
     */
    private long lastUpdateTime;
    /**
     * 下拉头的高度
     */
    private int hideHeaderHeight;
    /**
     * 下拉头的布局参数
     */
    private MarginLayoutParams headerLayoutParams;
    /**
     * 需要去下拉刷新的ListView
     */
    private AbsListView listView;

    /**
     * 手指按下时的屏幕纵坐标
     */
    private float yDown;

    public RefreshableView(Context context) {
        super(context);
        this.context = context;
    }

    public RefreshableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        //root:实例所要放入的跟视图
        //AttachToRoot:是否让此布局的layoutparams属性生效,当root的值为null时,true和false效果相同,false的话以后addView的时候会生效.true即时生效
        header = LayoutInflater.from(context).inflate(R.layout.pull_to_refresh, null, true);
        description = (TextView) header.findViewById(R.id.description);
        updateAt = (TextView) header.findViewById(R.id.update_at);
        progressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        arrow = (ImageView) header.findViewById(R.id.arrow);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        //根据事件切换显示更新时间
        refreshUpdateAtValue();
        setOrientation(VERTICAL);
        //添加头布局
        addView(header, 0);
    }

    /**
     * 进行一些关键性的初始化操作,比如:将下拉头向上偏移进行隐藏,给ListView注册touch事件
     *
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && !loadOnce) {
            hideHeaderHeight = -header.getHeight();
            headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
            //向上隐藏头布局
            headerLayoutParams.topMargin = hideHeaderHeight;
            loadOnce = true;
        }
    }

    private boolean result =false, state = false;
    public void initContainer(AbsListView absListView) {
        listView = absListView;
        listView.setOnTouchListener(this);
    }

    /**
     * 刷新下拉头中上次更新时间的文字描述。
     */
    private void refreshUpdateAtValue() {
        lastUpdateTime = preferences.getLong(UPDATED_AT + mId, -1);
        if (lastUpdateTime == -1) {
            lastUpdateTime = System.currentTimeMillis();
        }
//        long currentTime = System.currentTimeMillis();
//        long timePassed = currentTime - lastUpdateTime;
//        long timeIntoFormat;
//        String updateAtValue;
//        String value;
//        if (lastUpdateTime == -1) {
//            updateAtValue = getResources().getString(R.string.not_updated_yet);
//        } else if (timePassed < 0) {
//            updateAtValue = getResources().getString(R.string.time_error);
//        } else if (timePassed < ONE_MINUTE) {
//            updateAtValue = getResources().getString(R.string.updated_just_now);
//        } else if (timePassed < ONE_HOUR) {
//            timeIntoFormat = timePassed / ONE_MINUTE;
//            value = timeIntoFormat + "分钟";
//            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
//        } else if (timePassed < ONE_DAY) {
//            timeIntoFormat = timePassed / ONE_HOUR;
//            value = timeIntoFormat + "小时";
//            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
//        } else if (timePassed < ONE_MONTH) {
//            timeIntoFormat = timePassed / ONE_DAY;
//            value = timeIntoFormat + "天";
//            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
//        } else if (timePassed < ONE_YEAR) {
//            timeIntoFormat = timePassed / ONE_MONTH;
//            value = timeIntoFormat + "个月";
//            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
//        } else {
//            timeIntoFormat = timePassed / ONE_YEAR;
//            value = timeIntoFormat + "年";
//            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
//        }
        String label = DateUtils.formatDateTime(context, lastUpdateTime,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
        updateAt.setText(label);

    }

    /**
     * 当ListView被触摸时调用,其中处理了各种下拉刷新的具体逻辑
     *
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        setIsAbleToPull(event);
        if (ableToPull) {
            switch (event.getAction()) {

                case ACTION_DOWN:

                    yDown = event.getRawY();
                    break;

                case ACTION_MOVE:
                        result = true;
                        float yMove = event.getRawY();
                        int distance = (int) (yMove - yDown);
                        //如果手指是向下滑状态,并且下拉头完全是隐藏的,就屏蔽下拉事件

                        //手指上划,不拦截
                        if (distance <= 0 && headerLayoutParams.topMargin <= hideHeaderHeight) {
                            return false;
                        }
                        //小于敏感值不拦截
                        if (distance < touchSlop) {
                            return false;
                        }
                        //手指下滑
                        if (currentStatus != STATUS_REFRESHING) {
                            //jack 16/5/3  防止下拉过程中误触发长按事件和点击事件
                            clearContentViewEvents();
//                            event.setAction(MotionEvent.ACTION_CANCEL);
                            if (headerLayoutParams.topMargin > 0) {
                                //松开刷新
                                currentStatus = STATUS_RELEASE_TO_REFRESH;
                            } else {
                                //下拉刷新
                                currentStatus = STATUS_PULL_TO_REFRESH;
                            }
                            //通过偏移下拉头的topMargin值,来实现下拉效果
                            headerLayoutParams.topMargin = (distance / 4) + hideHeaderHeight;
                            //重新设置值
                            header.setLayoutParams(headerLayoutParams);
                        }


                    break;

                case ACTION_UP:
                default:
                    if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
                        //松手时如果是释放立即刷新状态,就去调用正在刷新的任务
                        new RefreshingTask().execute();
                    } else if (currentStatus == STATUS_PULL_TO_REFRESH) {
                        //松手的时候如果是下拉状态,就去隐藏下拉头
                        new HideHeaderTask().execute();
                    }
                    break;
            }
            //时刻记下更新下拉头中的信息
            if (currentStatus == STATUS_PULL_TO_REFRESH || currentStatus == STATUS_RELEASE_TO_REFRESH) {
                updateHeaderView();

                // 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态
                listView.setPressed(false);
                listView.setFocusable(false);
                listView.setFocusableInTouchMode(false);

                lastStatus = currentStatus;
                // 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件
                return true;
            }
        }
            return false;

    }

    /**
     * 通过反射修改字段去掉长按事件和点击事件
     */
    private void clearContentViewEvents()
    {
        try
        {
            Field[] fields = AbsListView.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++)
                if (fields[i].getName().equals("mPendingCheckForLongPress"))
                {
                    // mPendingCheckForLongPress是AbsListView中的字段，通过反射获取并从消息列表删除，去掉长按事件
                    fields[i].setAccessible(true);
                    listView.getHandler().removeCallbacks((Runnable) fields[i].get(listView));
                } else if (fields[i].getName().equals("mTouchMode"))
                {
                    // TOUCH_MODE_REST = -1， 这个可以去除点击事件
                    fields[i].setAccessible(true);
                    fields[i].set(listView, -1);
                }
            // 去掉焦点
            ((AbsListView) listView).getSelector().setState(new int[]
                    { 0 });
        } catch (Exception e)
        {
            Log.d(TAG, "error : " + e.toString());
        }
    }

    /**
     * 更新头布局
     */
    private void updateHeaderView() {
        if (lastStatus != currentStatus) {
            if (currentStatus == STATUS_PULL_TO_REFRESH) {
                description.setText(getResources().getString(R.string.pull_to_refresh_pull_label));
                arrow.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
                description.setText(getResources().getString(R.string.pull_to_refresh_release_label));
                arrow.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentStatus == STATUS_REFRESHING) {
                description.setText(getResources().getString(R.string.pull_to_refresh_refreshing_label));
                arrow.clearAnimation();
                arrow.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
            refreshUpdateAtValue();
        }
    }

    /**
     * 根据当前的状态来旋转箭头
     */
    private void rotateArrow() {
        float pivotX = arrow.getWidth() / 2f;
        float pivotY = arrow.getHeight() / 2f;
        float fromDegress = 0f;
        float toDegress = 0f;
        if (currentStatus == STATUS_PULL_TO_REFRESH) {
            fromDegress = 180f;
            toDegress = 360f;
        } else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
            fromDegress = 0f;
            toDegress = 180f;
        }
        RotateAnimation animation = new RotateAnimation(fromDegress, toDegress,pivotX,pivotY);
        animation.setDuration(500);
        animation.setFillAfter(true);
        arrow.startAnimation(animation);
    }

    /**
     * 根据当前ListView的滚动状态来设定的值,每次都需要在onTouch中第一个执行,这样可以判断出当前应该是滚动ListView，还是应该进行下拉。
     *
     * @param event
     */
    private void setIsAbleToPull(MotionEvent event) {
        View firstChild = listView.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = listView.getFirstVisiblePosition();
            if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
                if (!ableToPull) {
                    yDown = event.getRawY();
                }
                // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
                ableToPull = true;
            } else {
                if (headerLayoutParams.topMargin != hideHeaderHeight) {
                    headerLayoutParams.topMargin = hideHeaderHeight;
                    header.setLayoutParams(headerLayoutParams);
                }
                ableToPull = false;
            }
        } else {
            // 如果ListView中没有元素，也应该允许下拉刷新
            ableToPull = true;
        }
    }

    /**
     * 使当前线程睡眠指定的毫秒数
     * @param i
     *          指定当前线程睡眠多久,毫秒为单位
     */
    private void sleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行刷新的异步任务站
     */
    private class RefreshingTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (true) {
                topMargin = topMargin + SCROLL_SPEED;
                if (topMargin <= 0) {
                    topMargin = 0;
                    break;
                }
                publishProgress(topMargin);
                MyUtil.Sleep(10);
            }
            currentStatus = STATUS_REFRESHING;
            publishProgress(0);
            if (mListener !=null){
                mListener.onRefresh();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateHeaderView();
            headerLayoutParams.topMargin = values[0];
            header.setLayoutParams(headerLayoutParams);
        }
    }




    /**
     * 隐藏头布局的异步任务站
     */
    private class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (true) {
                topMargin = topMargin + SCROLL_SPEED;
                if (topMargin <= hideHeaderHeight) {
                    topMargin = hideHeaderHeight;
                    break;
                }
                publishProgress(topMargin);
                MyUtil.Sleep(10);
            }
            return topMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            headerLayoutParams.topMargin = values[0];
            header.setLayoutParams(headerLayoutParams);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            headerLayoutParams.topMargin = integer;
            header.setLayoutParams(headerLayoutParams);
            currentStatus = STATUS_REFRESH_FINISHED;
        }
    }

    /**
     * 下拉刷新的监听器，使用下拉刷新的地方应该注册此监听器来获取刷新回调。
     *
     * @author guolin
     */
    public interface PullToRefreshListener {

        /**
         * 刷新时会去回调此方法，在方法内编写具体的刷新逻辑。注意此方法是在子线程中调用的， 你可以不必另开线程来进行耗时操作。
         */
        void onRefresh();

    }
    /**
     * 给下拉刷新控件注册一个监听器。
     *
     * @param listener
     *            监听器的实现。
     * @param id
     *            为了防止不同界面的下拉刷新在上次更新时间上互相有冲突， 请不同界面在注册下拉刷新监听器时一定要传入不同的id。
     */
    public void setOnRefreshListener(PullToRefreshListener listener, int id) {
        mListener = listener;
        mId = id;
    }
    /**
     * 当所有的刷新逻辑完成后，记录调用一下，否则你的ListView将一直处于正在刷新状态。
     */
    public void finishRefreshing() {
        currentStatus = STATUS_REFRESH_FINISHED;
        preferences.edit().putLong(UPDATED_AT + mId, System.currentTimeMillis()).commit();
        new HideHeaderTask().execute();
    }
}
