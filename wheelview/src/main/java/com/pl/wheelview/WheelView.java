package com.pl.wheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.pl.whellview.R;

import java.util.ArrayList;

public class WheelView extends View {
    private static final String TAG="WheelView";
    /**
     * 刷新界面
     */
    private static final int REFRESH_VIEW = 0x001;
    /**
     * 持续滑动刷新界面
     */
    private static final int GO_ON_MOVE_REFRESH=10010;
    /**
     * 持续滑动刷新界面结束
     */
    private static final int GO_ON_MOVE_END=10011;
    /**
     * 打断持续滑动
     */
    private static final int GO_ON_MOVE_INTERRUPTED=10012;
    /**
     * 控件宽度
     */
    private float controlWidth;//px
    /**
     * 控件高度
     */
    private float controlHeight;//px
    /**
     * measure的高度，用于校正尺寸变化引起的文字错乱
     */
    private float lastMeasuredHeight;
    /**
     * 是否滑动中
     */
    private boolean isScrolling = false;
    /**
     * 选择的内容
     */
    private ArrayList<ItemObject> itemList = new ArrayList<>();
    /**
     * 设置数据
     */
    private ArrayList<String> dataList = new ArrayList<>();
    /**
     * 按下的坐标
     */
    private int downY;
    /**
     * 按下的时间
     */
    private long downTime = 0;//ms
    /**
     * 快速移动的时间
     */
//    private long goonTime = 200;//ms
    /**
     * 快速移动的距离
     */
//    private static final int GOON_MIN_DISTANCE =30;//dp
//    private int goOnMinDistance;//px
    /**
     * 缓慢滚动的时候的速度
     */
    public static final int SLOW_MOVE_SPEED = 3; //px
    /**
     * 画线画笔
     */
    private Paint linePaint;
    /**
     * 线的默认颜色
     */
    private int lineColor = 0xff000000;
    /**
     * 线的默认宽度
     */
    private float lineHeight = 2f;//px
    /**
     * 默认字体
     */
    private float normalFont = 14.0f;//px
    /**
     * 选中的时候字体
     */
    private float selectedFont = 22.0f;//px
    /**
     * 单元格高度
     */
    private float unitHeight = 50;//px
    /**
     * 显示多少个内容
     */
    private int itemNumber = 7;
    /**
     * 默认字体颜色
     */
    private int normalColor = 0xff000000;
    /**
     * 选中时候的字体颜色
     */
    private int selectedColor = 0xffff0000;
    /**
     * 选择监听
     */
    private OnSelectListener onSelectListener;
    /**
     * 是否可用
     */
    private boolean isEnable = true;
    /**
     * 快速滑动时，移动的基础个数
     */
    private static final int MOVE_NUMBER = 1;
    /**
     * 是否允许选空
     */
    private boolean noEmpty = true;

    /**
     * 正在修改数据，避免ConcurrentModificationException异常
     */
    private boolean isClearing = false;
    /**
     * 连续滑动使用的插值器
     */
    Interpolator goonInterpolator =new DecelerateInterpolator(2);
    /**
     * 连续滑动的距离，为unitHeight的整数倍
     */
    int goOnLimit;
    /**
     * 连续滑动动画的绘制间隔
     */
    private static final int GO_ON_REFRESH_INTERVAL_MILLIS = 10;
    /**
     * 连续滑动动画的最大绘制次数
     */
    private static final int SHOWTIME=300;
    /**
     * 连续滑动动画的绘制计数
     */
    private int showTime=0;
    /**
     * 保存最近一次连续滑动的距离的原始值
     */
    private int goOnMove;
    /**
     * 当前连续滑动中已经滑动的距离
     */
    private int goOnDistance;
    /**
     * 是否正在连续滑动状态中
     */
    private boolean isGoOnMove=false;
    /**
     * 滑动动画的HandlerThread
     */
    private HandlerThread moveHandlerThread;
    /**
     * 用于计算滑动动画位置的Handler，保证同一时刻只有一个滑动
     */
    private Handler moveHandler;

    public WheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
        initData();
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        initData();
    }

    public WheelView(Context context) {
        super(context);
        initData();
    }

    /**
     * 初始化，获取设置的属性
     *
     * @param context
     * @param attrs
     */
    private void init(Context context, AttributeSet attrs) {

        TypedArray attribute = context.obtainStyledAttributes(attrs, R.styleable.WheelView);
        unitHeight = (int) attribute.getDimension(R.styleable.WheelView_unitHeight, unitHeight);
        //按理说应该是奇数，但是此次不做判断，使用者有义务设置正确
        itemNumber = attribute.getInt(R.styleable.WheelView_itemNumber, itemNumber);

        normalFont = attribute.getDimension(R.styleable.WheelView_normalTextSize, normalFont);
        selectedFont = attribute.getDimension(R.styleable.WheelView_selectedTextSize, selectedFont);
        normalColor = attribute.getColor(R.styleable.WheelView_normalTextColor, normalColor);
        selectedColor = attribute.getColor(R.styleable.WheelView_selectedTextColor, selectedColor);

        lineColor = attribute.getColor(R.styleable.WheelView_lineColor, lineColor);
        lineHeight = attribute.getDimension(R.styleable.WheelView_lineHeight, lineHeight);

        noEmpty = attribute.getBoolean(R.styleable.WheelView_noEmpty, true);
        isEnable = attribute.getBoolean(R.styleable.WheelView_isEnable, true);

        attribute.recycle();

//        goOnMinDistance = (int) (context.getResources().getDisplayMetrics().density* GOON_MIN_DISTANCE);

        controlHeight = itemNumber * unitHeight;
        lastMeasuredHeight=controlHeight;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        moveHandlerThread =new HandlerThread("goOnHandlerThread");
        moveHandlerThread.setPriority(Thread.MIN_PRIORITY);
        moveHandlerThread.start();
        moveHandler =new GoOnHandler(moveHandlerThread.getLooper());
    }

    @Override
    protected void onDetachedFromWindow() {
        //销毁线程和handler
        if (moveHandlerThread !=null&& moveHandlerThread.isAlive()){
            moveHandlerThread.quit();
            moveHandler =null;
        }
        super.onDetachedFromWindow();
    }

    private class GoOnHandler extends Handler{
        GoOnHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case GO_ON_MOVE_REFRESH:
//                    Log.d(TAG,"GO_ON_MOVE_REFRESH,showTime="+showTime);
                    if ((getSelected()==0&&goOnMove>0)||(getSelected()==dataList.size()-1&&goOnMove<0)||getSelected()==-1){
                        //如果滑动到边缘选项，且超过，则加速滑动回来
                        showTime+=8;
                    }
                    showTime++;
                    int lastDistance= goOnDistance;
                    goOnDistance = (int)(goonInterpolator.getInterpolation((float) showTime/(float)SHOWTIME)*goOnLimit);
                    actionThreadMove(goOnMove > 0 ? goOnDistance : goOnDistance * (-1));
                    if (showTime<SHOWTIME&&isGoOnMove&&Math.abs(lastDistance-goOnDistance)>SLOW_MOVE_SPEED){
                        //Math.abs(lastDistance-goOnDistance)>SLOW_MOVE_SPEED是为了让滚动更加连贯，
                        // 否则在slowMove函数中滚动的速度反而可能超过这里的滚动速度，会有卡了一下的感觉
                        moveHandler.sendEmptyMessageDelayed(GO_ON_MOVE_REFRESH, GO_ON_REFRESH_INTERVAL_MILLIS);
                    }else {
                        isGoOnMove=false;
                        moveHandler.sendEmptyMessage(GO_ON_MOVE_END);
                    }
                    break;
                case GO_ON_MOVE_END:
                    //对滑动的距离取uniHeight的整数倍，保证滑动停止后不需要大范围调整
//                    goOnDistance= (int) (Math.ceil(goOnDistance/unitHeight)*unitHeight);
                    slowMove(goOnMove > 0 ?goOnDistance:goOnDistance*(-1));
                    isScrolling = false;
                    isGoOnMove=false;
                    break;
                case GO_ON_MOVE_INTERRUPTED:
                    //在滑动的过程中被打断，则以当前已经滑动的而距离作为新的起点，继续下一次滑动
//                    Log.d(TAG,"GO_ON_MOVE_INTERRUPTED");
                    slowMove(goOnMove > 0 ?goOnDistance:goOnDistance*(-1));
                    for (ItemObject item : itemList) {
                        item.newY(goOnMove > 0 ?goOnDistance:goOnDistance*(-1));
                    }
                    isScrolling = false;
                    isGoOnMove=false;
                    break;
            }
        }
    }



    /**
     * 继续快速移动一段距离，连续滚动动画，滚动速度递减，速度减到SLOW_MOVE_SPEED之下后调用slowMove
     * @param time 滑动的时间间隔
     * @param move 滑动的距离
     */
    private synchronized void goonMove(long time, final long move) {
        goOnMove= (int) move;
        showTime=0;
        if (time<=0){
            time=1;
        }
        goOnLimit= (int) (unitHeight*(MOVE_NUMBER+Math.abs(move)*2/time)+unitHeight/3);
        isGoOnMove=true;
        //将MotionEvent.ACTION_MOVE引起的滑动的距离设置为新的起点，然后再开始新的滑动
        //防止重复滑动同一次Action_Down中滑动的部分
        for (ItemObject item : itemList) {
            item.newY((int) (unitHeight*Math.floor(item.move/unitHeight)));
        }
        moveHandler.sendEmptyMessage(GO_ON_MOVE_REFRESH);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable)
            return true;
        int y = (int) event.getY();
        int move = Math.abs(y - downY);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //防止被其他可滑动View抢占焦点，比如嵌套到ListView中使用时
                getParent().requestDisallowInterceptTouchEvent(true);
                if (isScrolling){
                    isGoOnMove=false;
                    if (moveHandler !=null) {
                        //清除当前快速滑动的动画，进入下一次滑动动作
                        moveHandler.removeMessages(GO_ON_MOVE_REFRESH);
                        moveHandler.sendEmptyMessage(GO_ON_MOVE_INTERRUPTED);
                    }
                }
                isScrolling = true;
                downY = (int) event.getY();
                downTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                isGoOnMove=false;
                isScrolling = true;
                actionMove(y - downY);
                onSelectListener();
                break;
            case MotionEvent.ACTION_UP:
                long time= System.currentTimeMillis()-downTime;
                // 判断这段时间移动的距离
//                Log.d(TAG,"time="+time+",move="+move+",y - downY="+(y - downY));
//                if (time < goonTime && move > goOnMinDistance) {
                if ((double)move/(double)time>0.5) {//用比值来判断更精准，有些超快超短的滑动也能识别
                    goonMove(time,y - downY);
                } else {
                    //如果移动距离为0，则认为是点击事件，否则认为是小距离滑动
                    if (move==0){
                        if (downY<unitHeight*(itemNumber/2)&&downY>0){
                            //如果不先move再up，而是直接up，则无法产生点击时的滑动效果
                            //通过调整move和up的距离，可以调整点击的效果
                            actionMove((int) (unitHeight/3));
                            slowMove((int) unitHeight/3);
                        }else if (downY>controlHeight-unitHeight*(itemNumber/2)&&downY<controlHeight){
                            actionMove(-(int) (unitHeight/3));
                            slowMove(-(int) unitHeight/3);
                        }
                    }else {
                        slowMove(y - downY);
                    }
                    isScrolling = false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 初始化数据
     */
    private void initData() {
        isClearing = true;
        itemList.clear();
        for (int i = 0; i < dataList.size(); i++) {
            ItemObject itemObject = new ItemObject();
            itemObject.id = i;
            itemObject.setItemText(dataList.get(i));
            itemObject.x = 0;
            itemObject.y = (int) (i * unitHeight);
            itemList.add(itemObject);
        }
        isClearing = false;
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode=MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode==MeasureSpec.AT_MOST){
            int atMostHeight=MeasureSpec.getSize(heightMeasureSpec);
            //AT_MOST模式下，如果最大值小于当前view高度，则缩小当前view高度，以适应窗口大小
            if (atMostHeight<controlHeight&&atMostHeight!=0 ){
                controlHeight=atMostHeight;
                unitHeight= (int) (controlHeight/itemNumber);
            }
        }else if (heightMode==MeasureSpec.EXACTLY){
            //EXACTLY模式下，就以设置的大小为准,调整当前View
            int height=MeasureSpec.getSize(heightMeasureSpec);
            controlHeight=height;
            unitHeight= (int) (controlHeight/itemNumber);
        }else if (heightMode==MeasureSpec.UNSPECIFIED){
            //UNSPECIFIED保持原状不变
        }

        int width=MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, (int) controlHeight);

        //用于解决尺寸变化引起的文字位置错乱的问题
        if (Math.abs(lastMeasuredHeight-controlHeight)>0.1){
            int index=getSelected();
            initData();
            if(index!=-1) {
                setDefault(index);
            }else {
                setDefault(defaultIndex);
            }
            lastMeasuredHeight=controlHeight;
        }
//        controlWidth = getWidth();
//        if (controlWidth != 0) {
//            setMeasuredDimension(getWidth(), (int) controlHeight);
//        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //因为在recycler中添加的时候会导致文字不居中
        controlWidth = getWidth();
        drawLine(canvas);
        drawList(canvas);
        drawMask(canvas);
    }

    /**
     * 绘制分隔线条
     *
     * @param canvas
     */
    private void drawLine(Canvas canvas) {

        if (linePaint == null) {
            linePaint = new Paint();
            linePaint.setColor(lineColor);
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(lineHeight);
        }

        canvas.drawLine(0, controlHeight / 2 - unitHeight / 2 + lineHeight,
                controlWidth, controlHeight / 2 - unitHeight / 2 + lineHeight, linePaint);
        canvas.drawLine(0, controlHeight / 2 + unitHeight / 2 - lineHeight,
                controlWidth, controlHeight / 2 + unitHeight / 2 - lineHeight, linePaint);
    }
    /**
     * 绘制待选项目
     *
     * @param canvas
     */
    private synchronized void drawList(Canvas canvas) {
        if (isClearing)
            return;
        try {
            for (ItemObject itemObject : itemList) {
                itemObject.drawSelf(canvas, getMeasuredWidth());
            }
        } catch (Exception e) {
        }
    }

    /**
     * 绘制上下的遮盖板
     *
     * @param canvas
     */
    private void drawMask(Canvas canvas) {
        LinearGradient lg = new LinearGradient(0, 0, 0, unitHeight, 0xD8ffffff,
                0xc0ffffff, TileMode.MIRROR);
        Paint paint = new Paint();
        paint.setShader(lg);
        canvas.drawRect(0, 0, controlWidth, itemNumber/2*unitHeight, paint);

        LinearGradient lg2 = new LinearGradient(0, controlHeight - unitHeight,
                0, controlHeight, 0xc0ffffff, 0xD8ffffff, TileMode.MIRROR);
        Paint paint2 = new Paint();
        paint2.setShader(lg2);
        canvas.drawRect(0, controlHeight - itemNumber/2*unitHeight, controlWidth,
                controlHeight, paint2);
    }



    /**
     * 不能为空，必须有选项,滑动动画结束时调用
     * 判断当前应该被选中的项目，如果其不在屏幕中间，则将其移动到屏幕中间
     * @param moveSymbol 移动的距离，实际上只需要其符号，用于判断当前滑动方向
     */
    private void noEmpty(int moveSymbol) {
        if (!noEmpty)
            return;
        // 将当前选择项目移动到正中间，防止出现偏差
        for (ItemObject item : itemList) {
            if (item.selected()){
                int move = (int) item.moveToSelected();
                defaultMove(move);
                if (onSelectListener != null)
                    onSelectListener.endSelect(item.id, item.getItemText());
                return;
            }
        }
        // 如果当前没有项目选中，则将根据滑动的方向，将最近的一项设为选中项目，并移动到正中间
        if (moveSymbol > 0) {
            for (int i = 0; i < itemList.size(); i++) {
                if (itemList.get(i).couldSelected()) {
                    int move = (int) itemList.get(i).moveToSelected();
                    defaultMove(move);
                    if (onSelectListener != null) {
                        onSelectListener.endSelect(itemList.get(i).id, itemList.get(i).getItemText());
                    }
                    return;
                }
            }
        } else {
            for (int i = itemList.size() - 1; i >= 0; i--) {
                if (itemList.get(i).couldSelected()) {
                    int move = (int) itemList.get(i).moveToSelected();
                    defaultMove(move);
                    if (onSelectListener != null) {
                        onSelectListener.endSelect(itemList.get(i).id, itemList.get(i).getItemText());
                    }
                    return;
                }
            }
        }
        //如果没有项目可被选中，则说明所有项目都在视图外，选择第一个或者最后一个为当前选中项
        int move = (int) itemList.get(0).moveToSelected();
        if (move < 0) {
            defaultMove(move);
        } else {
            defaultMove((int) itemList.get(itemList.size() - 1)
                    .moveToSelected());
        }
        for (ItemObject item : itemList) {
            if (item.selected()) {
                if (onSelectListener != null)
                    onSelectListener.endSelect(item.id, item.getItemText());
                break;
            }
        }
    }


    /**
     * 处理MotionEvent.ACTION_MOVE中的移动
     *
     * @param move 移动的距离
     */
    private void actionMove(int move) {
        for (ItemObject item : itemList) {
            item.move(move);
        }
        invalidate();
    }

    /**
     * 移动，线程中调用
     *
     * @param move 移动的距离
     */
    private void actionThreadMove(int move) {
        for (ItemObject item : itemList) {
            item.move(move);
        }
//        handler.sendEmptyMessage(REFRESH_VIEW);

        postInvalidate();
    }

    /**
     * 缓慢移动一段距离，移动速度为SLOW_MOVE_SPEED，
     * 注意这个距离不是move参数，而是先将选项坐标移动move的距离以后，再判断当前应该选中的项目，然后将改项目移动到中间
     * 移动完成后调用noEmpty
     * @param move 立即设置的新坐标移动距离，不是缓慢移动的距离
     */
    private synchronized void slowMove(final int move) {

        if (moveHandler ==null) {
            return;
        }
//        Log.d(TAG,"slowMove start");
        moveHandler.post(new Runnable() {
            @Override
            public void run() {
//                Log.d(TAG,"slowMove run start");
                int newMove = 0;
                //根据当前滑动方向，选择选中项来移到中心显示
                int selected=getSelected();
                if (selected!=-1){
                    newMove= (int) itemList.get(selected).moveToSelected();
                }else {
                    if (move > 0) {
                        for (int i = 0; i < itemList.size(); i++) {
                            if (itemList.get(i).couldSelected()) {
                                newMove = (int) itemList.get(i).moveToSelected();
                                break;
                            }
                        }
                    } else {
                        for (int i = itemList.size() - 1; i >= 0; i--) {
                            if (itemList.get(i).couldSelected()) {
                                newMove = (int) itemList.get(i).moveToSelected();
                                break;
                            }
                        }
                    }
                }
                for (ItemObject item : itemList) {
                    item.newY(move + 0);
                }

                // 判断正负
                int m = newMove > 0 ? newMove : newMove * (-1);
                int symbol = newMove > 0 ? 1 : (-1);
                // 移动速度
                int speed = SLOW_MOVE_SPEED;
                while (true && m!=0 ) {
                    m = m - speed;
                    if (m < 0) {
                        for (ItemObject item : itemList) {
                            item.newY(m * symbol);
                        }
//                        handler.sendEmptyMessage(REFRESH_VIEW);
                        postInvalidate();
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    for (ItemObject item : itemList) {
                        item.newY(speed * symbol);
                    }
//                    handler.sendEmptyMessage(REFRESH_VIEW);
                    postInvalidate();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
//                Log.d(TAG,"slowMove run end");
                noEmpty(move);
            }
        });
//        Log.d(TAG,"slowMove end");
    }

    /**
     * 移动到默认位置
     *
     * @param move 移动的距离
     */
    private void defaultMove(int move) {
        for (ItemObject item : itemList) {
            item.newY(move);
        }
//        handler.sendEmptyMessage(REFRESH_VIEW);
        postInvalidate();
    }

    /**
     * 滑动监听
     */
    private void onSelectListener() {
        if (onSelectListener == null)
            return;
        for (ItemObject item : itemList) {
            if (item.couldSelected()) {
                onSelectListener.selecting(item.id, item.getItemText());
            }
        }
    }

    /**
     * 设置数据 （第一次）
     *
     * @param data 数据集
     */
    public void setData(ArrayList<String> data) {
        this.dataList = data;
        initData();
    }

    /**
     * 重置数据
     *
     * @param data 数据集
     */
    public void refreshData(ArrayList<String> data) {
        setData(data);
        invalidate();
    }

    /**
     * 获取返回项 id
     *
     * @return
     */
    public int getSelected() {
        for (ItemObject item : itemList) {
            if (item.selected())
                return item.id;
        }
        return -1;
    }

    /**
     * 获取返回的内容
     *
     * @return
     */
    public String getSelectedText() {
        for (ItemObject item : itemList) {
            if (item.selected())
                return item.getItemText();
        }
        return "";
    }

    /**
     * 是否正在滑动
     *
     * @return
     */
    public boolean isScrolling() {
        return isScrolling;
    }

    /**
     * 是否可用
     *
     * @return
     */
    public boolean isEnable() {
        return isEnable;
    }

    /**
     * 设置是否可用
     *
     * @param isEnable
     */
    public void setEnable(boolean isEnable) {
        this.isEnable = isEnable;
    }

    /**
     * 设置默认选项
     *
     * @param index
     */
    public void setDefault(int index) {
        defaultIndex=index;
        if (index > itemList.size() - 1)
            return;
        float move = itemList.get(index).moveToSelected();
        defaultMove((int) move);
    }
    private int defaultIndex;//用于恢复

    /**
     * 获取列表大小
     *
     * @return
     */
    public int getListSize() {
        if (itemList == null)
            return 0;
        return itemList.size();
    }

    /**
     * 获取某项的内容
     *
     * @param index
     * @return
     */
    public String getItemText(int index) {
        if (itemList == null)
            return "";
        return itemList.get(index).getItemText();
    }

    /**
     * 对WheelView设置监听，在滑动过程或者滑动停止返回数据信息。
     * @param onSelectListener
     */
    public void setOnSelectListener(OnSelectListener onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

//    @SuppressLint("HandlerLeak")
//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            switch (msg.what) {
//                case REFRESH_VIEW:
//                    invalidate();
//                    break;
//                default:
//                    break;
//            }
//        }
//
//    };

    /**
     * 获取当前展示的项目数量
     * @return
     */
    public int getItemNumber() {
        return itemNumber;
    }

    /**
     * 设置展示的项目数量
     * @param itemNumber
     */
    public void setItemNumber(int itemNumber) {
        this.itemNumber = itemNumber;
        controlHeight = itemNumber * unitHeight;
        requestLayout();
    }

    private class ItemObject {
        /**
         * id
         */
        int id = 0;
        /**
         * 内容
         */
        private String itemText = "";
        /**
         * x坐标
         */
        int x = 0;
        /**
         * y坐标,代表静止时的位置
         */
        int y = 0;
        /**
         * 移动距离，代表滑动的位置，滑动结束后应该置0
         */
        int move = 0;
        /**
         * 字体画笔
         */
        private TextPaint textPaint;
        /**
         * 字体范围矩形
         */
        private Rect textRect;

        private boolean shouldRefreshTextPaint=true;

        /**
         * 绘制自身
         *
         * @param canvas         画板
         * @param containerWidth 容器宽度
         */
        public void drawSelf(Canvas canvas, int containerWidth) {


            // 判断是否可视
            // 通过将判断移到绘制的函数开始的位置，同时放宽判断的条件，可以减少计算量，提高性能
            if (!isInView()) {
                return;
            }

            // 返回包围整个字符串的最小的一个Rect区域
            if (textPaint==null) {
                textPaint = new TextPaint();
                textPaint.setAntiAlias(true);
            }
            if (textRect == null) {
                textRect = new Rect();
            }

            // 判断是否可被选择
            if (couldSelected()) {
                textPaint.setColor(selectedColor);
                // 获取距离标准位置的距离
                float moveToSelect = moveToSelected();
                moveToSelect = moveToSelect > 0 ? moveToSelect : moveToSelect * (-1);
                // 计算当前字体大小
                float textSize = normalFont
                        + ((selectedFont - normalFont) * (1.0f - moveToSelect / (float) unitHeight));
                textPaint.setTextSize(textSize);
            } else {
                textPaint.setColor(normalColor);
                textPaint.setTextSize(normalFont);
            }

            if (unitHeight<Math.max(selectedFont,normalFont)){
                //如果高度太小了，则调整字体大小，以匹配高度
                float textSize=unitHeight-lineHeight*2;
                textPaint.setTextSize(textSize);
            }

            if (shouldRefreshTextPaint) {
                //有可能导致文字消失，itemText变成空字符串，
                // 是因为文字设置过大，而containweWidth太小，
                // 本来会将无法显示的文字用"..."表示，但是连"..."本身也无法显示的时候，就会变成空字符串
                itemText = (String) TextUtils.ellipsize(itemText, textPaint, containerWidth, TextUtils.TruncateAt.END);
                textPaint.getTextBounds(itemText, 0, itemText.length(), textRect);
                if (selectedFont==normalFont) {
                    shouldRefreshTextPaint = false;
                }
            }

//            // 判断是否可视
//            if (!isInView()) {
//                return;
//            }

            // 绘制内容
            canvas.drawText(itemText, ((float)x + (float)controlWidth / 2f - (float)textRect.width() / 2f),
                    ((float)(y + move) + unitHeight / 2 + (float)textRect.height() / 2f), textPaint);

        }

        /**
         * 是否在可视界面内
         *
         * @return
         */
        public  synchronized boolean isInView() {

//            if (y + move > controlHeight || ((float)y + (float)move + (float)unitHeight / 2 + (float)textRect.height() / 2f) < 0)
            if (y + move > controlHeight || ((float)y + (float)move + (float)unitHeight  ) < 0)//放宽判断的条件，否则就不能再onDraw的开头执行，而要到textRect测量完成才能执行
                return false;
            return true;
        }

        /**
         * 移动一段距离
         *
         * @param _move 移动的距离
         */
        public synchronized void move(int _move) {
            this.move = _move;
        }

        /**
         * 设置新的坐标
         *
         * @param _move 移动的距离，叠加到当前坐标上
         */
        public  synchronized void newY(int _move) {
            this.move = 0;
            this.y = y + _move;
        }

        /**
         * 判断是否在可以选择区域内,用于在没有刚好被选中项的时候判断备选项
         * 考虑到文字的baseLine是其底部，而y+m的高度是文字的顶部的高度
         * 因此判断为可选区域的标准是需要减去文字的部分的
         * 也就是y+m在正中间和正中间上面一格的范围内，则判断为可选
         *
         * @return
         */
        public  synchronized boolean couldSelected() {
            boolean isSelect=true;
            if (y+move<=itemNumber/2*unitHeight-unitHeight||y+move>=itemNumber/2*unitHeight+unitHeight){
                isSelect=false;
            }
            return isSelect;
        }

        /**
         * 判断是否刚好在正中间的选择区域内
         *
         * @return
         */
        public  synchronized boolean selected() {
            boolean  isSelect=false;
            if (textRect==null){
                return false;
            }
            if ((y+move>=itemNumber/2*unitHeight-unitHeight/2+(float) textRect.height()/2)&&
                    (y+move<=itemNumber/2*unitHeight+unitHeight/2-(float)textRect.height()/2))
                isSelect=true;
            return isSelect;
        }

        public String getItemText() {
            return itemText;
        }

        public void setItemText(String itemText) {
            shouldRefreshTextPaint=true;
            this.itemText = itemText;
        }

        /**
         * 获取移动到选中位置需要的距离
         */
        public synchronized  float moveToSelected() {
            return (controlHeight / 2 - unitHeight / 2) - (y + move);
        }
    }

    public interface OnSelectListener {
        /**
         * 结束选择，滑动停止时回调
         *
         * @param id
         * @param text
         */
        void endSelect(int id, String text);

        /**
         * 选中的内容，滑动的过程中会不断回调
         *
         * @param id
         * @param text
         */
        void selecting(int id, String text);

    }
}
