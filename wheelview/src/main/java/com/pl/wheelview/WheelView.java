package com.pl.wheelview;

import android.annotation.SuppressLint;
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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.pl.whellview.R;

import java.util.ArrayList;

/**
 * WheelView滚轮
 *
 * @author JiangPing
 */
public class WheelView extends View {
    private static final String TAG="WheelView";

    private static final int GO_ON_MOVE_REFRESH=10010;
    private static final int GO_ON_MOVE_END=10011;
    private static final int GO_ON_MOVE_INTERRUPTED=10012;
    /**
     * 控件宽度
     */
    private float controlWidth;
    /**
     * 控件高度
     */
    private float controlHeight;
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
    private long downTime = 0;
    /**
     * 短促移动
     */
    private long goonTime = 200;
        /**
     * 短促移动距离
     */
    private int goonDistance = 100;
    /**
     * 点击移动距离
     */
    private int clickDistance = 5;
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
    private float lineHeight = 2f;
    /**
     * 默认字体
     */
    private float normalFont = 14.0f;
    /**
     * 选中的时候字体
     */
    private float selectedFont = 22.0f;
    /**
     * 单元格高度
     */
    private float unitHeight = 50;
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
     * 蒙板高度
     */
    private float maskHeight = 48.0f;
    /**
     * 选择监听
     */
    private OnSelectListener onSelectListener;
    /**
     * 是否可用
     */
    private boolean isEnable = true;
    /**
     * 刷新界面
     */
    private static final int REFRESH_VIEW = 0x001;
    /**
     * 移动距离
     */
    private static final int MOVE_NUMBER = 2;
    /**
     * 是否允许选空
     */
    private boolean noEmpty = true;

    /**
     * 正在修改数据，避免ConcurrentModificationException异常
     */
    private boolean isClearing = false;


    private HandlerThread goOnHandlerThread;
    private Handler goOnHandler;

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
        itemNumber = attribute.getInt(R.styleable.WheelView_itemNumber, itemNumber);

        normalFont = attribute.getDimension(R.styleable.WheelView_normalTextSize, normalFont);
        selectedFont = attribute.getDimension(R.styleable.WheelView_selectedTextSize, selectedFont);
        normalColor = attribute.getColor(R.styleable.WheelView_normalTextColor, normalColor);
        selectedColor = attribute.getColor(R.styleable.WheelView_selectedTextColor, selectedColor);

        lineColor = attribute.getColor(R.styleable.WheelView_lineColor, lineColor);
        lineHeight = attribute.getDimension(R.styleable.WheelView_lineHeight, lineHeight);

        maskHeight = attribute.getDimension(R.styleable.WheelView_maskHeight, maskHeight);
        noEmpty = attribute.getBoolean(R.styleable.WheelView_noEmpty, true);
        isEnable = attribute.getBoolean(R.styleable.WheelView_isEnable, true);

        attribute.recycle();

        controlHeight = itemNumber * unitHeight;
        lastMeasuredHeight=controlHeight;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        goOnHandlerThread=new HandlerThread("goOnHandlerThread");
        goOnHandlerThread.setPriority(Thread.MIN_PRIORITY);
        goOnHandlerThread.start();
        goOnHandler=new GoOnHandler(goOnHandlerThread.getLooper());
    }

    @Override
    protected void onDetachedFromWindow() {
        if (goOnHandlerThread!=null&&goOnHandlerThread.isAlive()){
            goOnHandlerThread.quit();
            goOnHandler=null;
        }
        super.onDetachedFromWindow();
    }

    class GoOnHandler extends Handler{

        GoOnHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case GO_ON_MOVE_REFRESH:
                    if ((getSelected()==0&&goOnMove>0)||(getSelected()==dataList.size()-1&&goOnMove<0)||getSelected()==-1){
                        showTime+=3;
                    }
                    showTime++;
                    goOnDistance = (int)(interpolator.getInterpolation((float) showTime/(float)SHOWTIME)*goOnLimit);
                    actionThreadMove(goOnMove > 0 ? goOnDistance : goOnDistance * (-1));
                    if (showTime<SHOWTIME&&isGoOnMove){
                        goOnHandler.sendEmptyMessageDelayed(GO_ON_MOVE_REFRESH,5);
                    }else {
                        isGoOnMove=false;
                        goOnHandler.sendEmptyMessage(GO_ON_MOVE_END);
                    }
                    break;
                case GO_ON_MOVE_END:
                    goOnDistance= (int) (Math.ceil(goOnDistance/unitHeight)*unitHeight);
                    actionUp(goOnMove > 0 ?goOnDistance:goOnDistance*(-1));
                    isScrolling = false;
                    isGoOnMove=false;
                    break;
                case GO_ON_MOVE_INTERRUPTED:
                    goOnDistance= (int) (Math.ceil(goOnDistance/unitHeight)*unitHeight);
                    for (ItemObject item : itemList) {
                        item.newY(goOnMove > 0 ?goOnDistance:goOnDistance*(-1));
                    }
                    isScrolling = false;
                    isGoOnMove=false;
                    break;
            }
        }
    }

    Interpolator interpolator=new DecelerateInterpolator(2);
    int goOnLimit;
    int showTime=0;
    int goOnMove;
    boolean isGoOnMove=false;
    int goOnDistance;
    private static final int SHOWTIME=200;

    /**
     * 继续移动一定距离
     */
    private synchronized void goonMove(final long time, final long move) {
        goOnMove= (int) move;
        showTime=0;
        goOnLimit= (int) (unitHeight*(MOVE_NUMBER+Math.abs(move)*2/time));
        isGoOnMove=true;
        goOnHandler.sendEmptyMessage(GO_ON_MOVE_REFRESH);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable)
            return true;
        int y = (int) event.getY();
        int move = Math.abs(y - downY);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (isScrolling){
                    isGoOnMove=false;
                    if (goOnHandler!=null) {
                        goOnHandler.removeMessages(GO_ON_MOVE_REFRESH);
                        goOnHandler.sendEmptyMessage(GO_ON_MOVE_INTERRUPTED);
                    }
                }
                isScrolling = true;
                downY = (int) event.getY();
                downTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                isGoOnMove=false;
                isScrolling = true;
                if (move>=clickDistance) {
                    actionMove(y - downY);
                }
                onSelectListener();
                break;
            case MotionEvent.ACTION_UP:
                long time= System.currentTimeMillis()-downTime;
                // 判断这段时间移动的距离
                if (time < goonTime && move > goonDistance) {
                    goonMove(time,y - downY);
                } else {
                    if (move<=clickDistance){
                        if (downY<unitHeight*(itemNumber/2)&&downY>0){
                            actionMove((int) (unitHeight/2));
                            actionUp((int) unitHeight/3);
                        }else if (downY>controlHeight-unitHeight*(itemNumber/2)&&downY<controlHeight){
                            actionMove(-(int) (unitHeight/2));
                            actionUp(-(int) unitHeight/3);
                        }
                    }else {
                        actionUp(y - downY);
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

    private float lastMeasuredHeight;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode=MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode==MeasureSpec.AT_MOST){
            int atMostHeight=MeasureSpec.getSize(heightMeasureSpec);
            if (atMostHeight<controlHeight&&atMostHeight!=0 ){
                controlHeight=atMostHeight;
                unitHeight= (int) (controlHeight/itemNumber);
                maskHeight= (int) (controlHeight/itemNumber);
            }
        }else if (heightMode==MeasureSpec.EXACTLY){
            int height=MeasureSpec.getSize(heightMeasureSpec);
            controlHeight=height;
            unitHeight= (int) (controlHeight/itemNumber);
            maskHeight= (int) (controlHeight/itemNumber);
        }else if (heightMode==MeasureSpec.UNSPECIFIED){
//            return;
        }
//        heightMeasureSpec=MeasureSpec.makeMeasureSpec((int) controlHeight,heightMode);
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width=MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, (int) controlHeight);

        //用于解决尺寸变化引起的转轮文字位置错乱的问题
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
     * 绘制线条
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
     * 绘制遮盖板
     *
     * @param canvas
     */
    private void drawMask(Canvas canvas) {
        LinearGradient lg = new LinearGradient(0, 0, 0, maskHeight, 0xD8ffffff,
                0xc0ffffff, TileMode.MIRROR);
        Paint paint = new Paint();
        paint.setShader(lg);
        canvas.drawRect(0, 0, controlWidth, maskHeight, paint);

        LinearGradient lg2 = new LinearGradient(0, controlHeight - maskHeight,
                0, controlHeight, 0xc0ffffff, 0xD8ffffff, TileMode.MIRROR);
        Paint paint2 = new Paint();
        paint2.setShader(lg2);
        canvas.drawRect(0, controlHeight - maskHeight, controlWidth,
                controlHeight, paint2);
    }



    /**
     * 不能为空，必须有选项
     */
    private void noEmpty(int moveSymbol) {
        if (!noEmpty)
            return;
        //将当前选择项目移动到正中间，防止出现偏差
        for (ItemObject item : itemList) {
            if (item.selected()){
                int move = (int) item.moveToSelected();
                defaultMove(move);
                if (onSelectListener != null)
                    onSelectListener.endSelect(item.id, item.getItemText());
                return;
            }
        }
        // 如果当前没有项目选中，则将根据滑动的方向，将最近的设为选择项目，并移动到正中间
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
     * 移动的时候
     *
     * @param move
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
     * @param move
     */
    private void actionThreadMove(int move) {
        for (ItemObject item : itemList) {
            item.move(move);
        }
        handler.sendEmptyMessage(REFRESH_VIEW);
    }

    /**
     * 松开的时候
     *
     * @param move
     */
    private void actionUp(int move) {
        Log.d(TAG,"action up start");

        slowMove(move);
        handler.sendEmptyMessage(REFRESH_VIEW);

        Log.d(TAG,"action up end");
    }

    /**
     * 缓慢移动
     *
     * @param move
     */
    private synchronized void slowMove(final int move) {

        if (goOnHandler==null) {
            return;
        }
        Log.d(TAG,"slowMove start");
        goOnHandler.post(new Runnable() {
            @Override
            public void run() {
                int newMove = 0;
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
                for (ItemObject item : itemList) {
                    item.newY(move + 0);
                }

                Log.d(TAG,"slowMove run start");
                // 判断正负
                int m = newMove > 0 ? newMove : newMove * (-1);
                int symbol = newMove > 0 ? 1 : (-1);
                // 移动速度
                int speed = 5;
                while (true && m!=0 ) {
                    m = m - speed;
                    if (m < 0) {
                        for (ItemObject item : itemList) {
                            item.newY(m * symbol);
                        }
                        handler.sendEmptyMessage(REFRESH_VIEW);
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
                    handler.sendEmptyMessage(REFRESH_VIEW);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG,"slowMove run end");
//                goOnHandler.removeCallbacksAndMessages(null);
//                goOnHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                    }
//                });
                        noEmpty(move);
            }
        });
        Log.d(TAG,"slowMove end");
    }

    /**
     * 移动到默认位置
     *
     * @param move
     */
    private void defaultMove(int move) {
        for (ItemObject item : itemList) {
            item.newY(move);
        }
        handler.sendEmptyMessage(REFRESH_VIEW);
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
     * @param data
     */
    public void setData(ArrayList<String> data) {
        this.dataList = data;
        initData();
    }

    /**
     * 重置数据
     *
     * @param data
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
     * 监听
     *
     * @param onSelectListener
     */
    public void setOnSelectListener(OnSelectListener onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_VIEW:
                    invalidate();
                    break;
                default:
                    break;
            }
        }

    };

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
     * @param adjustMaskHeight 是否同步调整蒙版高度，除了正中间的一个之外都被遮挡，true为调整
     */
    public void setItemNumber(int itemNumber,boolean adjustMaskHeight) {
        this.itemNumber = itemNumber;
        controlHeight = itemNumber * unitHeight;
        if (adjustMaskHeight) {
            maskHeight = (itemNumber / 2) * unitHeight;
        }
        requestLayout();
    }

    /**
     * 单条内容
     *
     * @author JiangPing
     */
    private class ItemObject {
        /**
         * id
         */
        public int id = 0;
        /**
         * 内容
         */
        private String itemText = "";
        /**
         * x坐标
         */
        public int x = 0;
        /**
         * y坐标
         */
        public int y = 0;
        /**
         * 移动距离
         */
        public int move = 0;
        /**
         * 字体画笔
         */
        private TextPaint textPaint;
        /**
         * 字体范围矩形
         */
        private Rect textRect;

        private boolean isSelect=false;
        private boolean shouldRefreshTextPaint=true;
        public ItemObject() {
            super();
        }

        /**
         * 绘制自身
         *
         * @param canvas         画板
         * @param containerWidth 容器宽度
         */
        public void drawSelf(Canvas canvas, int containerWidth) {
            // 判断是否可视

            // 返回包围整个字符串的最小的一个Rect区域
            if (textPaint==null) {
                textPaint = new TextPaint();
                textPaint.setAntiAlias(true);
            }
            if (textRect == null) {
                textRect = new Rect();
            }

            // 判断是否被选择
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

                itemText = (String) TextUtils.ellipsize(itemText, textPaint, containerWidth, TextUtils.TruncateAt.END);
                textPaint.getTextBounds(itemText, 0, itemText.length(), textRect);
                shouldRefreshTextPaint=false;
            }

            if (!isInView()) {
                return;
            }

            // 绘制内容
            canvas.drawText(itemText, ((float)x + (float)controlWidth / 2f - (float)textRect.width() / 2f),
                    ((float)y + (float)move + (float)unitHeight / 2 + (float)textRect.height() / 2f), textPaint);

        }

        /**
         * 是否在可视界面内
         *
         * @return
         */
        public  synchronized boolean isInView() {
            if (y + move > controlHeight || ((float)y + (float)move + (float)unitHeight / 2 + (float)textRect.height() / 2f) < 0)
                return false;
            return true;
        }

        /**
         * 移动距离
         *
         * @param _move
         */
        public synchronized void move(int _move) {
            this.move = _move;
        }

        /**
         * 设置新的坐标
         *
         * @param _move
         */
        public  synchronized void newY(int _move) {
            this.move = 0;
            this.y = y + _move;
        }

        /**
         * 判断是否在可以选择区域内
         *
         * @return
         */
        public  synchronized boolean couldSelected() {
                isSelect=true;
//                if ((y+move)>=(controlHeight-unitHeight*((itemNumber)/2+1.5f)+(float)textRect.height() / 2f)&&
//                        (y+move)<=(controlHeight-unitHeight*(itemNumber/2-0.5f)-(float)textRect.height() / 2f)){
                if (y+move<=itemNumber/2*unitHeight-unitHeight||y+move>=itemNumber/2*unitHeight+unitHeight){
                    isSelect=false;
                }
            return isSelect;
        }

        /**
         * 判断是否刚好在选择区域内
         *
         * @return
         */
        public  synchronized boolean selected() {
            isSelect=false;
//            if ((y+move)>=(controlHeight-unitHeight*((itemNumber/2)+1))&&
//                    (y+move)<=(controlHeight-unitHeight*(itemNumber/2))){
//                isSelect=true;
//            }
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
         * 获取移动到标准位置需要的距离
         */
        public synchronized  float moveToSelected() {
            return (controlHeight / 2 - unitHeight / 2) - (y + move);
        }
    }

    /**
     * 选择监听
     *
     * @author JiangPing
     */
    public interface OnSelectListener {
        /**
         * 结束选择
         *
         * @param id
         * @param text
         */
        void endSelect(int id, String text);

        /**
         * 选中的内容
         *
         * @param id
         * @param text
         */
        void selecting(int id, String text);

    }
}
