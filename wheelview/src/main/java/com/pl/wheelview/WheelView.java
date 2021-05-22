package com.pl.wheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.pl.whellview.R;
import java.util.ArrayList;

public class WheelView extends FrameLayout {

  private static final String TAG = "WheelView";
  /** 刷新界面 */
  private static final int REFRESH_VIEW = 0x001;
  /** 持续滑动刷新界面 */
  private static final int GO_ON_MOVE_REFRESH = 10010;
  /** 持续滑动刷新界面结束 */
  private static final int GO_ON_MOVE_END = 10011;
  /** 打断持续滑动 */
  private static final int GO_ON_MOVE_INTERRUPTED = 10012;

  public static final int DEFAULT_MASK_DARK_COLOR = 0xD8ffffff;
  public static final int DEFAULT_MASK_LIGHT_COLOR = 0xc0ffffff;
  /** 控件宽度 */
  private float controlWidth; // px
  /** 控件高度 */
  private float controlHeight; // px
  /** measure的高度，用于校正尺寸变化引起的文字错乱 */
  private float lastMeasuredHeight;
  /** 是否滑动中 */
  private boolean isScrolling = false;
  /** 选择的内容 */
  private ArrayList<ItemObject> itemList = new ArrayList<>();
  /** 设置数据 */
  private ArrayList<String> dataList = new ArrayList<>();
  /** 按下的Y坐标 */
  private int downY;
  /** 上一次MotionEvent的Y坐标 */
  private int lastY;
  /** 按下的时间 */
  private long downTime = 0; // ms
  /** 当前设备的density */
  private float density = 1;
  /** 缓慢滚动的时候的速度 */
  private static final int SLOW_MOVE_SPEED = 1; // dp

  private int slowMoveSpeed = SLOW_MOVE_SPEED;
  /** 判断为点击的移动距离 */
  private static final int CLICK_DISTANCE = 2; // dp

  private int clickDistance = CLICK_DISTANCE;
  /** 判断为点击的时间间隔 */
  private int clickTimeout = 100; // ms
  /** 滚动的最大速度 */
  private int mMaximumFlingVelocity;
  /** 滚动的最小速度 */
  private int mMinimumFlingVelocity;
  /** 滑动速度检测器 */
  private VelocityTracker mVelocityTracker;
  /** 画线画笔 */
  private Paint linePaint;

  private Paint mastPaint;
  /** 线的默认颜色 */
  private int lineColor = 0xff000000;
  /** 线的默认宽度 */
  private float lineHeight = 2f; // px
  /** 线的默认宽度 */
  private float maxWidth = -1; // px
  /** 默认字体 */
  private float normalFont = 14.0f; // px
  /** 选中的时候字体 */
  private float selectedFont = 22.0f; // px
  /** 单元格高度 */
  private float unitHeight = 50; // px
  /** 显示多少个内容 */
  private int itemNumber = 7;
  /** 默认字体颜色 */
  private int normalColor = 0xff000000;
  /** 选中时候的字体颜色 */
  private int selectedColor = 0xffff0000;

  /** 遮罩的深色颜色 */
  private int maskDarkColor = DEFAULT_MASK_DARK_COLOR;

  /** 遮罩的浅色部分 */
  private int maskLightColor = DEFAULT_MASK_LIGHT_COLOR;
  /** 选择监听 */
  private OnSelectListener onSelectListener;
  /** 输入监听 */
  private onInputListener onInputListener;
  /** 是否可用 */
  private boolean isEnable = true;
  /** 快速滑动时，移动的基础个数 */
  private static final int MOVE_NUMBER = 1;
  /** 是否允许选空 */
  private boolean noEmpty = true;

  /** 设定的是否循环滚动 */
  private boolean isCyclic = true;
  /** 真实使用的是否循环滚动——当item个数少于展示个数时，强制不使用循环滚动 */
  private boolean _isCyclic = true;

  /** 正在修改数据，避免ConcurrentModificationException异常 */
  private boolean isClearing = false;
  /** 连续滑动使用的插值器 */
  Interpolator goonInterpolator = new DecelerateInterpolator(2);
  /** 连续滑动的距离，为unitHeight的整数倍 */
  int goOnLimit;
  /** 连续滑动动画的绘制间隔 */
  private static final int GO_ON_REFRESH_INTERVAL_MILLIS = 10;
  /** 连续滑动动画的最大绘制次数 */
  private static final int SHOWTIME = 200;
  /** 连续滑动动画的绘制计数 */
  private int showTime = 0;
  /** 保存最近一次连续滑动的距离的原始值 */
  private int goOnMove;
  /** 当前连续滑动中已经滑动的距离 */
  private int goOnDistance;
  /** 是否正在连续滑动状态中 */
  private boolean isGoOnMove = false;
  /** 滑动动画的HandlerThread */
  private HandlerThread moveHandlerThread;
  /** 用于计算滑动动画位置的Handler，保证同一时刻只有一个滑动 */
  private Handler moveHandler;
  /** 所有item的移动距离，用同一个变量记录，减少计算 */
  private int moveDistance;

  /** 是否允许使用文字输入 */
  private boolean withInputText;

  /** The text for showing the current value. */
  private EditText mInputText;

  private Handler callbackHandler;
  private LinearGradient linearGradientUp;
  private LinearGradient linearGradientDown;

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
    this(context, null);
  }

  /** 初始化，获取设置的属性 */
  private void init(final Context context, AttributeSet attrs) {

    TypedArray attribute = context.obtainStyledAttributes(attrs, R.styleable.WheelView);
    unitHeight = (int) attribute.getDimension(R.styleable.WheelView_unitHeight, unitHeight);
    // 按理说应该是奇数，但是此次不做判断，使用者有义务设置正确
    itemNumber = attribute.getInt(R.styleable.WheelView_itemNumber, itemNumber);

    normalFont = attribute.getDimension(R.styleable.WheelView_normalTextSize, normalFont);
    selectedFont = attribute.getDimension(R.styleable.WheelView_selectedTextSize, selectedFont);
    normalColor = attribute.getColor(R.styleable.WheelView_normalTextColor, normalColor);
    selectedColor = attribute.getColor(R.styleable.WheelView_selectedTextColor, selectedColor);

    lineColor = attribute.getColor(R.styleable.WheelView_lineColor, lineColor);
    lineHeight = attribute.getDimension(R.styleable.WheelView_lineHeight, lineHeight);
    maxWidth = attribute.getDimension(R.styleable.WheelView_android_maxWidth, -1);

    noEmpty = attribute.getBoolean(R.styleable.WheelView_noEmpty, true);
    isEnable = attribute.getBoolean(R.styleable.WheelView_isEnable, true);
    isCyclic = attribute.getBoolean(R.styleable.WheelView_isCyclic, true);
    withInputText = attribute.getBoolean(R.styleable.WheelView_withInputText, false);

    maskDarkColor =
        attribute.getColor(R.styleable.WheelView_maskDarkColor, DEFAULT_MASK_DARK_COLOR);
    maskLightColor =
        attribute.getColor(R.styleable.WheelView_maskLightColor, DEFAULT_MASK_LIGHT_COLOR);

    density = context.getResources().getDisplayMetrics().density;
    slowMoveSpeed = (int) (density * SLOW_MOVE_SPEED);
    clickDistance = (int) (density * CLICK_DISTANCE);

    controlHeight = itemNumber * unitHeight;

    toShowItems = new ItemObject[itemNumber + 2];

    ViewConfiguration configuration = ViewConfiguration.get(context);
    //        clickDistance = configuration.getScaledTouchSlop();
    clickTimeout = configuration.getTapTimeout();
    mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
    mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();

    callbackHandler = new Handler(Looper.getMainLooper());

    int inputColor = attribute.getColor(R.styleable.WheelView_inputTextColor, Color.BLACK);
    int background = attribute.getResourceId(R.styleable.WheelView_inputBackground, 0);
    mInputText = new EditText(context);
    if (background == 0) {
      mInputText.setBackgroundColor(Color.TRANSPARENT);
    } else {
      mInputText.setBackgroundResource(background);
    }
    mInputText.setTextColor(inputColor);
    mInputText.setTextSize(TypedValue.COMPLEX_UNIT_PX, selectedFont);
    mInputText.setGravity(Gravity.CENTER);
    mInputText.setPadding(0, 0, 0, 0);
    mInputText.setOnFocusChangeListener(
        new OnFocusChangeListener() {
          public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
              mInputText.setText(getSelectedText());
              showInputMethod(context);
              if (onInputListener != null) {
                onInputListener.onStartInput(WheelView.this, mInputText, getSelectedText());
              }
              mInputText.selectAll();
            } else {
              mInputText.setSelection(0, 0);
              mInputText.setVisibility(GONE);
              if (onInputListener != null) {
                onInputListener.endInput(
                    WheelView.this, mInputText, mInputText.getText().toString());
              }
              hideInputMethod(mInputText);
            }
          }
        });
    int inputType = EditorInfo.TYPE_NULL;
    inputType = attribute.getInt(R.styleable.WheelView_android_inputType, EditorInfo.TYPE_NULL);
    mInputText.setInputType(inputType);
    mInputText.setImeOptions(EditorInfo.IME_ACTION_DONE);
    mInputText.setOnEditorActionListener(
        new OnEditorActionListener() {
          @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
              mInputText.setVisibility(GONE);
              if (onInputListener != null) {
                onInputListener.endInput(
                    WheelView.this, mInputText, mInputText.getText().toString());
              }
              return true;
            }
            return false;
          }
        });

    LayoutParams layoutParams =
        new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) unitHeight);
    layoutParams.gravity = Gravity.CENTER;
    layoutParams.leftMargin =
        attribute.getDimensionPixelOffset(R.styleable.WheelView_inputMarginLeft, 0);
    layoutParams.rightMargin =
        attribute.getDimensionPixelOffset(R.styleable.WheelView_inputMarginRight, 0);
    Log.e(TAG, "leftMargin=" + layoutParams.leftMargin);
    Log.e(TAG, "rightMargin=" + layoutParams.rightMargin);
    addView(mInputText, layoutParams);
    mInputText.setVisibility(GONE);
    setWillNotDraw(false);
    attribute.recycle();
  }

  private void showInputMethod(Context context) {
    InputMethodManager imm =
        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(mInputText, InputMethodManager.SHOW_IMPLICIT);
  }

  public static void hideInputMethod(View view) {
    InputMethodManager imm =
        (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    moveHandlerThread = new HandlerThread("goOnHandlerThread");
    moveHandlerThread.setPriority(Thread.MIN_PRIORITY);
    moveHandlerThread.start();
    moveHandler = new GoOnHandler(moveHandlerThread.getLooper());
  }

  @Override
  protected void onDetachedFromWindow() {
    // 销毁线程和handler
    if (moveHandlerThread != null && moveHandlerThread.isAlive()) {
      moveHandlerThread.quit();
    }
    super.onDetachedFromWindow();
  }

  private void _setIsCyclic(boolean cyclic) {
    if (dataList.size() < itemNumber + 2) {
      _isCyclic = false;
    } else {
      _isCyclic = cyclic;
    }
  }

  private class GoOnHandler extends Handler {

    GoOnHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      int lastDistance = goOnDistance;
      if (moveHandler == null) {
        return;
      }
      switch (msg.what) {
        case GO_ON_MOVE_REFRESH:
          //                    Log.d(TAG,"GO_ON_MOVE_REFRESH,showTime="+showTime);
          showTime++;
          goOnDistance =
              (int)
                  (goonInterpolator.getInterpolation((float) showTime / (float) SHOWTIME)
                      * goOnLimit);
          actionThreadMove(
              goOnMove > 0 ? (goOnDistance - lastDistance) : (goOnDistance - lastDistance) * (-1));
          if (showTime < SHOWTIME
              && isGoOnMove
              && (showTime < SHOWTIME / 5
                  || Math.abs(lastDistance - goOnDistance) >= slowMoveSpeed)) {
            // Math.abs(lastDistance-goOnDistance)>SLOW_MOVE_SPEED是为了让滚动更加连贯，showTime<SHOWTIME/5是为了防止刚启动时的误判
            // 否则在slowMove函数中滚动的速度反而可能超过这里的滚动速度，会有卡了一下的感觉
            moveHandler.sendEmptyMessageDelayed(GO_ON_MOVE_REFRESH, GO_ON_REFRESH_INTERVAL_MILLIS);
          } else {
            //
            // Log.d(TAG,"lastDistance-goOnDistance="+(lastDistance-goOnDistance));
            isGoOnMove = false;
            moveHandler.sendEmptyMessage(GO_ON_MOVE_END);
          }
          break;
        case GO_ON_MOVE_END:
          //                    Log.d(TAG,"GO_ON_MOVE_END,goOnDistance="+goOnDistance);
          slowMove(goOnMove > 0 ? slowMoveSpeed : ((slowMoveSpeed) * (-1)));
          isScrolling = false;
          isGoOnMove = false;
          goOnDistance = 0;
          goOnLimit = 0;
          break;
        case GO_ON_MOVE_INTERRUPTED:
          // 在滑动的过程中被打断，则以当前已经滑动的而距离作为新的起点，继续下一次滑动
          //                    Log.d(TAG,"GO_ON_MOVE_INTERRUPTED");
          moveDistance +=
              goOnMove > 0 ? (goOnDistance - lastDistance) : (goOnDistance - lastDistance) * (-1);
          goOnDistance = 0;
          isScrolling = false;
          isGoOnMove = false;
          findItemsToShow();
          postInvalidate();
          break;
      }
    }
  }

  /**
   * 继续快速移动一段距离，连续滚动动画，滚动速度递减，速度减到SLOW_MOVE_SPEED之下后调用slowMove
   *
   * @param velocity 滑动的初始速度
   * @param move 滑动的距离
   */
  private synchronized void goonMove(int velocity, final long move) {
    showTime = 0;
    int newGoonMove = (int) (Math.abs(velocity / 10));
    if (goOnMove * move > 0) {
      goOnLimit += newGoonMove;
    } else {
      goOnLimit = newGoonMove;
    }
    goOnMove = (int) move;
    isGoOnMove = true;
    // 将MotionEvent.ACTION_MOVE引起的滑动的距离设置为新的起点，然后再开始新的滑动
    // 防止重复滑动同一次Action_Down中滑动的部分
    if (moveHandler == null) {
      return;
    }
    moveHandler.sendEmptyMessage(GO_ON_MOVE_REFRESH);
    //        Log.d(TAG,"goonMove : newGoonMove="+newGoonMove);
    //        Log.d(TAG,"goonMove : goOnLimit="+goOnLimit);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!isEnable) {
      return true;
    }
    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(event);

    int y = (int) event.getY();
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        mInputText.setVisibility(GONE);
        mInputText.clearFocus();
        // 防止被其他可滑动View抢占焦点，比如嵌套到ListView中使用时
        getParent().requestDisallowInterceptTouchEvent(true);
        if (isScrolling) {
          isGoOnMove = false;
          if (moveHandler != null) {
            // 清除当前快速滑动的动画，进入下一次滑动动作
            moveHandler.removeMessages(GO_ON_MOVE_REFRESH);
            moveHandler.sendEmptyMessage(GO_ON_MOVE_INTERRUPTED);
          }
        }
        isScrolling = true;
        downY = (int) event.getY();
        lastY = (int) event.getY();
        downTime = System.currentTimeMillis();
        break;
      case MotionEvent.ACTION_MOVE:
        isGoOnMove = false;
        isScrolling = true;
        actionMove(y - lastY);
        lastY = y;
        break;
      case MotionEvent.ACTION_UP:
        long time = System.currentTimeMillis() - downTime;
        // 判断这段时间移动的距离
        //                Log.d(TAG,"time="+time+",y - downY="+(y - downY));

        // 用速度来判断是非快速滑动
        VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
        int initialVelocity = (int) velocityTracker.getYVelocity();
        if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
          goonMove(initialVelocity, y - downY);
        } else {
          // 如果移动距离较小，则认为是点击事件，否则认为是小距离滑动
          if (Math.abs(y - downY) <= clickDistance && time <= clickTimeout) {
            if (downY < unitHeight * (itemNumber / 2) && downY > 0) {
              // 如果不先move再up，而是直接up，则无法产生点击时的滑动效果
              // 通过调整move和up的距离，可以调整点击的效果
              actionMove((int) (unitHeight / 3));
              slowMove((int) unitHeight * 2 / 3);
            } else if (downY > controlHeight - unitHeight * (itemNumber / 2)
                && downY < controlHeight) {
              actionMove(-(int) (unitHeight / 3));
              slowMove(-(int) unitHeight * 2 / 3);
            } else {
              if (withInputText) {
                mInputText.setVisibility(VISIBLE);
                mInputText.requestFocus();
              }
              noEmpty(y - downY);
            }
          } else {
            slowMove(y - downY);
          }
          isScrolling = false;
        }

        mVelocityTracker.recycle();
        mVelocityTracker = null;
        break;
      default:
        break;
    }
    return true;
  }

  /** 初始化数据 */
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
    _setIsCyclic(isCyclic);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    if (heightMode == MeasureSpec.AT_MOST) {
      int atMostHeight = MeasureSpec.getSize(heightMeasureSpec);
      // AT_MOST模式下，如果最大值小于当前view高度，则缩小当前view高度，以适应窗口大小
      if (atMostHeight < controlHeight && atMostHeight != 0) {
        controlHeight = atMostHeight;
        unitHeight = (int) (controlHeight / itemNumber);
      }
    } else if (heightMode == MeasureSpec.EXACTLY) {
      // EXACTLY模式下，就以设置的大小为准,调整当前View
      int height = MeasureSpec.getSize(heightMeasureSpec);
      controlHeight = height;
      unitHeight = (int) (controlHeight / itemNumber);
    } else if (heightMode == MeasureSpec.UNSPECIFIED) {
      // UNSPECIFIED保持原状不变
    }

    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int width = MeasureSpec.getSize(widthMeasureSpec);

    if (widthMode == MeasureSpec.AT_MOST) {
      int atMostWidth = MeasureSpec.getSize(widthMeasureSpec);
      // AT_MOST模式下，如果最大值小于当前view高度，则缩小当前view高度，以适应窗口大小
      TextPaint textPaint = new TextPaint();
      textPaint.setTextSize(selectedFont);
      Rect textRect = new Rect();
      int maxWidth = 0;
      for (String s : dataList) {
        s = s + "PA"; // 增加一点长度，否则显示会有点问题
        textPaint.getTextBounds(s, 0, s.length(), textRect);
        if (maxWidth < textRect.width()) {
          maxWidth = textRect.width();
        }
      }
      if (atMostWidth > maxWidth + getPaddingLeft() + getPaddingRight() && maxWidth != 0) {
        width = maxWidth + getPaddingLeft() + getPaddingRight();
      }
    } else if (widthMode == MeasureSpec.EXACTLY) {
      // EXACTLY模式下，就以设置的大小为准,直接使用
    } else if (widthMode == MeasureSpec.UNSPECIFIED) {
      // UNSPECIFIED保持原状不变,直接使用
    }
    if (maxWidth != -1 && width > maxWidth) {
      width = (int) maxWidth;
    }

    setMeasuredDimension(width, (int) controlHeight);

    mInputText.measure(
        MeasureSpec.makeMeasureSpec(
            getMeasuredWidth()
                - ((LayoutParams) mInputText.getLayoutParams()).leftMargin
                - ((LayoutParams) mInputText.getLayoutParams()).rightMargin,
            MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec((int) (controlHeight / itemNumber), MeasureSpec.EXACTLY));

    // 用于解决尺寸变化引起的文字位置错乱的问题
    if (Math.abs(lastMeasuredHeight - controlHeight) > 0.1) {
      int index = getSelected();
      initData();
      if (index != -1) {
        setDefault(index);
      } else {
        setDefault(defaultIndex);
      }
      lastMeasuredHeight = controlHeight;
      mInputText.getLayoutParams().height = (int) (controlHeight / itemNumber);
    }
    //        controlWidth = getWidth();
    //        if (controlWidth != 0) {
    //            setMeasuredDimension(getWidth(), (int) controlHeight);
    //        }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    // 因为在recycler中添加的时候会导致文字不居中
    controlWidth = getWidth();
    drawLine(canvas);
    drawList(canvas);
    drawMask(canvas);
  }

  /** 绘制分隔线条 */
  private void drawLine(Canvas canvas) {

    if (linePaint == null) {
      linePaint = new Paint();
      linePaint.setColor(lineColor);
      linePaint.setAntiAlias(true);
      linePaint.setStrokeWidth(lineHeight);
    }

    canvas.drawLine(
        0,
        controlHeight / 2 - unitHeight / 2 + lineHeight,
        controlWidth,
        controlHeight / 2 - unitHeight / 2 + lineHeight,
        linePaint);
    canvas.drawLine(
        0,
        controlHeight / 2 + unitHeight / 2 - lineHeight,
        controlWidth,
        controlHeight / 2 + unitHeight / 2 - lineHeight,
        linePaint);
  }

  /** 绘制待选项目 */
  private synchronized void drawList(Canvas canvas) {
    if (isClearing) {
      return;
    }
    synchronized (toShowItems) {
      for (ItemObject itemObject : toShowItems) {
        if (itemObject != null) {
          itemObject.drawSelf(canvas, getMeasuredWidth());
        }
      }
    }
  }

  /** 绘制上下的遮盖板 */
  private void drawMask(Canvas canvas) {
    if (mastPaint == null) {
      mastPaint = new Paint();
      linearGradientUp =
          new LinearGradient(0, 0, 0, unitHeight, maskDarkColor, maskLightColor, TileMode.CLAMP);
      linearGradientDown =
          new LinearGradient(
              0,
              controlHeight - unitHeight,
              0,
              controlHeight,
              maskLightColor,
              maskDarkColor,
              TileMode.CLAMP);
    }

    mastPaint.setShader(linearGradientUp);
    canvas.drawRect(0, 0, controlWidth, itemNumber / 2 * unitHeight, mastPaint);

    mastPaint.setShader(linearGradientDown);
    canvas.drawRect(
        0, controlHeight - itemNumber / 2 * unitHeight, controlWidth, controlHeight, mastPaint);
  }

  /**
   * 不能为空，必须有选项,滑动动画结束时调用 判断当前应该被选中的项目，如果其不在屏幕中间，则将其移动到屏幕中间
   *
   * @param moveSymbol 移动的距离，实际上只需要其符号，用于判断当前滑动方向
   */
  private void noEmpty(int moveSymbol) {
    if (!noEmpty) {
      return;
    }
    // 将当前选择项目移动到正中间，防止出现偏差

    //        Log.d(TAG,"noEmpty start");
    synchronized (toShowItems) {
      findItemsToShow();
      for (ItemObject item : toShowItems) {
        if (item != null) {
          if (item.selected()) {
            int move = (int) item.moveToSelected();
            onEndSelecting(item);
            defaultMove(move);
            //                        Log.d(TAG, "noEmpty selected=" + item.id+",movoToSelected=
            // "+move);
            return;
          }
        }
      }
      // 如果当前没有项目选中，则将根据滑动的方向，将最近的一项设为选中项目，并移动到正中间
      if (moveSymbol > 0) {
        for (int i = 0; i < toShowItems.length; i++) {
          if (toShowItems[i] != null && toShowItems[i].couldSelected()) {
            int move = (int) toShowItems[i].moveToSelected();
            onEndSelecting(toShowItems[i]);
            defaultMove(move);
            //                        Log.d(TAG, "noEmpty couldSelected=" +
            // toShowItems[i].id+",movoToSelected= "+move);
            return;
          }
        }
      } else {
        for (int i = toShowItems.length - 1; i >= 0; i--) {
          if (toShowItems[i] != null && toShowItems[i].couldSelected()) {
            int move = (int) toShowItems[i].moveToSelected();
            onEndSelecting(toShowItems[i]);
            defaultMove(move);
            //                        Log.d(TAG, "noEmpty couldSelected=" +
            // toShowItems[i].id+",movoToSelected= "+move);
            return;
          }
        }
      }
    }
  }

  private void onEndSelecting(final ItemObject toShowItem) {
    if (onSelectListener != null) {
      callbackHandler.removeCallbacksAndMessages(null);
      callbackHandler.post(
          new Runnable() {
            @Override
            public void run() {
              onSelectListener.endSelect(toShowItem.id, toShowItem.getRawText());
            }
          });
    }
  }

  /**
   * 处理MotionEvent.ACTION_MOVE中的移动
   *
   * @param move 移动的距离
   */
  private void actionMove(int move) {
    moveDistance -= move;
    //        Log.d(TAG,"move="+move+",moveDistance="+moveDistance);
    findItemsToShow();
    invalidate();
  }

  /**
   * 移动，线程中调用
   *
   * @param move 移动的距离
   */
  private void actionThreadMove(int move) {
    moveDistance -= move;
    findItemsToShow();
    postInvalidate();
  }

  /** 找到将会显示的几个item */
  private ItemObject[] toShowItems; // 其长度等于itemNumber+2

  private void findItemsToShow() {
    findItemsToShow(true);
  }

  private void findItemsToShow(boolean callListener) {
    if (itemList.isEmpty()) {
      return;
    }
    if (_isCyclic) {
      // 循环模式下，将moveDistance限定在一定的范围内循环变化，同时要保证滚动的连续性
      if (moveDistance > unitHeight * itemList.size()) {
        moveDistance = moveDistance % ((int) unitHeight * itemList.size());
      } else if (moveDistance < 0) {
        moveDistance =
            moveDistance % ((int) unitHeight * itemList.size())
                + (int) unitHeight * itemList.size();
      }
      int move = moveDistance;
      ItemObject first = itemList.get(0);
      int firstY = first.y + move;
      int firstNumber = (int) (Math.abs(firstY / unitHeight)); // 滚轮中显示的第一个item的index
      int restMove = (int) (firstY - unitHeight * firstNumber); // 用以保证滚动的连续性
      int takeNumberStart = firstNumber;

      synchronized (toShowItems) {
        for (int i = 0; i < toShowItems.length; i++) {
          int takeNumber = takeNumberStart + i;
          int realNumber = takeNumber;
          if (takeNumber < 0) {
            realNumber = itemList.size() + takeNumber; // 调整循环滚动显示的index
          } else if (takeNumber >= itemList.size()) {
            realNumber = takeNumber - itemList.size(); // 调整循环滚动显示的index
          }
          toShowItems[i] = itemList.get(realNumber);
          toShowItems[i].move(
              (int) (unitHeight * ((i - realNumber) % itemList.size())) - restMove); // 设置滚动的相对位置
          //            Log.e(TAG," toShowItems["+i+"] = "+ toShowItems[i].id);
        }
        //
        // Log.e(TAG,"---------------------------------------------------------------------------------------------------------");
      }
    } else {
      // 非循环模式下，滚动到边缘即停止动画
      if (moveDistance > unitHeight * itemList.size() - itemNumber / 2 * unitHeight - unitHeight) {
        moveDistance =
            (int) (unitHeight * itemList.size() - itemNumber / 2 * unitHeight - unitHeight);
        if (moveHandler != null) {
          moveHandler.removeMessages(GO_ON_MOVE_REFRESH);
          moveHandler.sendEmptyMessage(GO_ON_MOVE_INTERRUPTED);
        }
      } else if (moveDistance < -itemNumber / 2 * unitHeight) {
        moveDistance = (int) (-itemNumber / 2 * unitHeight);
        if (moveHandler != null) {
          moveHandler.removeMessages(GO_ON_MOVE_REFRESH);
          moveHandler.sendEmptyMessage(GO_ON_MOVE_INTERRUPTED);
        }
      }

      int move = moveDistance;
      ItemObject first = itemList.get(0);

      int firstY = first.y + move;
      int firstNumber = (int) (firstY / unitHeight); // 滚轮中显示的第一个item的index
      int restMove = (int) (firstY - unitHeight * firstNumber); // 用以保证滚动的连续性
      int takeNumberStart = firstNumber;
      synchronized (toShowItems) {
        for (int i = 0; i < toShowItems.length; i++) {
          int takeNumber = takeNumberStart + i;
          int realNumber = takeNumber;
          if (takeNumber < 0) {
            realNumber = -1; // 用以标识超出的部分
          } else if (takeNumber >= itemList.size()) {
            realNumber = -1; // 用以标识超出的部分
          }
          if (realNumber == -1) {
            toShowItems[i] = null; // 设置为null，则会留出空白
          } else {
            toShowItems[i] = itemList.get(realNumber);
            toShowItems[i].move((int) (unitHeight * (i - realNumber)) - restMove); // 设置滚动的相对位置
          }
          //            Log.e(TAG," toShowItems["+i+"] = "+ toShowItems[i].id);
        }
        //
        // Log.e(TAG,"---------------------------------------------------------------------------------------------------------");
      }
    }

    // 调用回调
    if (callListener && onSelectListener != null && toShowItems[itemNumber / 2] != null) {
      callbackHandler.post(
          new Runnable() {
            @Override
            public void run() {
              onSelectListener.selecting(
                  toShowItems[itemNumber / 2].id, toShowItems[itemNumber / 2].getRawText());
            }
          });
    }
  }

  /**
   * 缓慢移动一段距离，移动速度为SLOW_MOVE_SPEED， 注意这个距离不是move参数，而是先将选项坐标移动move的距离以后，再判断当前应该选中的项目，然后将改项目移动到中间
   * 移动完成后调用noEmpty
   *
   * @param move 立即设置的新坐标移动距离，不是缓慢移动的距离
   */
  private synchronized void slowMove(final int move) {

    if (moveHandler == null) {
      return;
    }
    //        Log.d(TAG,"slowMove start");
    moveHandler.post(
        new Runnable() {
          @Override
          public void run() {
            //                Log.d(TAG,"slowMove run start");
            int newMove = 0;
            findItemsToShow();
            // 根据当前滑动方向，选择选中项来移到中心显示
            int selected = getSelected();
            if (selected != -1) {
              newMove = (int) itemList.get(selected).moveToSelected();
              //                    Log.e(TAG,"getSelected:"+selected+"  , newMove="+newMove);
            } else {
              synchronized (toShowItems) {
                if (move > 0) {
                  for (int i = 0; i < toShowItems.length; i++) {
                    if (toShowItems[i] != null && toShowItems[i].couldSelected()) {
                      newMove = (int) toShowItems[i].moveToSelected();
                      //                                    Log.e(TAG, "move > 0 couldSelected:" +
                      // toShowItems[i].id);
                      break;
                    }
                  }
                } else {
                  for (int i = toShowItems.length - 1; i >= 0; i--) {
                    if (toShowItems[i] != null && toShowItems[i].couldSelected()) {
                      newMove = (int) toShowItems[i].moveToSelected();
                      //                                    Log.e(TAG, "move < 0 couldSelected:" +
                      // toShowItems[i].id);
                      break;
                    }
                  }
                }
              }
            }
            // 判断正负
            int m = newMove > 0 ? newMove : newMove * (-1);
            int symbol = newMove > 0 ? 1 : (-1);
            // 移动速度
            int speed = slowMoveSpeed;
            while (true && m != 0) {
              m = m - speed;
              if (m < 0) {
                moveDistance -= m * symbol;
                findItemsToShow();
                postInvalidate();
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
                break;
              }
              moveDistance -= speed * symbol;
              findItemsToShow();
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
    //        Log.d(TAG,"slowMove end move="+move);
  }

  /**
   * 移动到默认位置
   *
   * @param move 移动的距离
   */
  private void defaultMove(int move) {
    moveDistance -= move;
    findItemsToShow(false);
    postInvalidate();
  }

  /**
   * 设置数据 （第一次）
   *
   * @param data 数据集
   */
  public void setData(ArrayList<String> data) {
    this.dataList = data;
    initData();
    requestLayout();
  }

  /**
   * 重置数据
   *
   * @param data 数据集
   */
  public void refreshData(ArrayList<String> data) {
    setData(data);
    findItemsToShow();
    invalidate();
  }

  /** 获取返回项 id */
  public int getSelected() {
    synchronized (toShowItems) {
      for (ItemObject item : toShowItems) {
        if (item != null && item.selected()) {
          return item.id;
        }
      }
      return -1;
    }
  }

  /** 获取返回的内容 */
  public String getSelectedText() {
    synchronized (toShowItems) {
      for (ItemObject item : toShowItems) {
        if (item != null && item.selected()) {
          return item.getRawText();
        }
      }
      return "";
    }
  }

  /** 是否正在滑动 */
  public boolean isScrolling() {
    return isScrolling;
  }

  /** 是否可用 */
  public boolean isEnable() {
    return isEnable;
  }

  /** 设置是否可用 */
  public void setEnable(boolean isEnable) {
    this.isEnable = isEnable;
  }

  /** 设置默认选项 */
  public void setDefault(int index) {
    defaultIndex = index;
    if (itemList.isEmpty()) {
      return;
    }
    if (index > itemList.size() - 1) {
      return;
    }
    moveDistance = 0;
    for (ItemObject item : itemList) {
      item.move = 0;
    }
    findItemsToShow();
    float move = itemList.get(index).moveToSelected();
    onEndSelecting(itemList.get(index));
    defaultMove((int) move);
  }

  private int defaultIndex; // 用于恢复

  /** 获取列表大小 */
  public int getListSize() {
    if (itemList == null) {
      return 0;
    }
    return itemList.size();
  }

  /** 获取某项的内容 */
  public String getItemText(int index) {
    if (itemList == null) {
      return "";
    }
    return itemList.get(index).getRawText();
  }

  /** 对WheelView设置监听，在滑动过程或者滑动停止返回数据信息。 */
  public void setOnSelectListener(OnSelectListener onSelectListener) {
    this.onSelectListener = onSelectListener;
  }

  public void setOnInputListener(WheelView.onInputListener onInputListener) {
    this.onInputListener = onInputListener;
  }

  /** 获取当前展示的项目数量 */
  public int getItemNumber() {
    return itemNumber;
  }

  /** 设置展示的项目数量 */
  public void setItemNumber(int itemNumber) {
    this.itemNumber = itemNumber;
    controlHeight = itemNumber * unitHeight;
    toShowItems = new ItemObject[itemNumber + 2];
    requestLayout();
  }

  /** 获取是否循环滚动 */
  public boolean isCyclic() {
    return isCyclic;
  }

  /** 设置是否循环滚动 */
  public void setCyclic(boolean cyclic) {
    isCyclic = cyclic;
    _setIsCyclic(cyclic);
  }

  public boolean isWithInputText() {
    return withInputText;
  }

  public void setWithInputText(boolean withInputText) {
    this.withInputText = withInputText;
  }

  private class ItemObject {

    /** id */
    int id = 0;
    /** 内容 */
    private String itemText = "";
    /** 原始内容，itemText可能会缩短成...，导致使用时出错 */
    private String rawText = "";
    /** x坐标 */
    int x = 0;
    /** y坐标,代表绝对位置，由id和unitHeight决定 */
    int y = 0;
    /** 移动距离，代表滑动的相对位置，用以调整当前位置 */
    int move = 0;
    /** 字体画笔 */
    private TextPaint textPaint;
    /** 字体范围矩形 */
    private Rect textRect;

    private boolean shouldRefreshTextPaint = true;

    /**
     * 绘制自身
     *
     * @param canvas 画板
     * @param containerWidth 容器宽度
     */
    public void drawSelf(Canvas canvas, int containerWidth) {

      // 判断是否可视
      // 通过将判断移到绘制的函数开始的位置，同时放宽判断的条件，可以减少计算量，提高性能
      if (!isInView()) {
        return;
      }

      // 返回包围整个字符串的最小的一个Rect区域
      if (textPaint == null) {
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
        float textSize =
            normalFont + ((selectedFont - normalFont) * (1.0f - moveToSelect / (float) unitHeight));
        textPaint.setTextSize(textSize);
      } else {
        textPaint.setColor(normalColor);
        textPaint.setTextSize(normalFont);
      }

      if (unitHeight < Math.max(selectedFont, normalFont)) {
        // 如果高度太小了，则调整字体大小，以匹配高度
        float textSize = unitHeight - lineHeight * 2;
        textPaint.setTextSize(textSize);
        mInputText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
      }

      if (shouldRefreshTextPaint) {
        // 有可能导致文字消失，itemText变成空字符串，
        // 是因为文字设置过大，而containerWidth太小，
        // 本来会将无法显示的文字用"..."表示，但是连"..."本身也无法显示的时候，就会变成空字符串
        itemText =
            (String)
                TextUtils.ellipsize(itemText, textPaint, containerWidth, TextUtils.TruncateAt.END);
        textPaint.getTextBounds(itemText, 0, itemText.length(), textRect);
        if (selectedFont == normalFont) {
          shouldRefreshTextPaint = false;
        }
      }

      //            // 判断是否可视
      //            if (!isInView()) {
      //                return;
      //            }

      // 绘制内容
      canvas.drawText(
          itemText,
          ((float) x + (float) controlWidth / 2f - (float) textRect.width() / 2f),
          ((float) (y + move) + unitHeight / 2 + (float) textRect.height() / 2f),
          textPaint);
    }

    /** 是否在可视界面内 */
    public synchronized boolean isInView() {

      //            if (y + move > controlHeight || ((float)y + (float)move + (float)unitHeight / 2
      // + (float)textRect.height() / 2f) < 0)
      if (y + move > controlHeight
          || ((float) y + (float) move + (float) unitHeight)
              < 0) // 放宽判断的条件，否则就不能再onDraw的开头执行，而要到textRect测量完成才能执行
      {
        return false;
      }
      return true;
    }

    /**
     * 设置相对移动的位置
     *
     * @param _move 移动的距离
     */
    public synchronized void move(int _move) {
      this.move = _move;
    }

    /**
     * 判断是否在可以选择区域内,用于在没有刚好被选中项的时候判断备选项 考虑到文字的baseLine是其底部，而y+m的高度是文字的顶部的高度 因此判断为可选区域的标准是需要减去文字的部分的
     * 也就是y+m在正中间和正中间上面一格的范围内，则判断为可选
     */
    public synchronized boolean couldSelected() {
      boolean isSelect = true;
      if (y + move <= itemNumber / 2 * unitHeight - unitHeight
          || y + move >= itemNumber / 2 * unitHeight + unitHeight) {
        isSelect = false;
      }
      return isSelect;
    }

    /** 判断是否刚好在正中间的选择区域内 */
    public synchronized boolean selected() {
      boolean isSelect = false;
      if (textRect == null) {
        return false;
      }
      if ((y + move >= itemNumber / 2 * unitHeight - unitHeight / 2 + (float) textRect.height() / 2)
          && (y + move
              <= itemNumber / 2 * unitHeight + unitHeight / 2 - (float) textRect.height() / 2)) {
        isSelect = true;
      }
      return isSelect;
    }

    public String getRawText() {
      return rawText;
    }

    public void setItemText(String itemText) {
      shouldRefreshTextPaint = true;
      this.itemText = itemText;
      this.rawText = new String(itemText);
    }

    /** 获取移动到选中位置需要的距离 */
    public synchronized float moveToSelected() {
      return (controlHeight / 2 - unitHeight / 2) - (y + move);
    }
  }

  public interface OnSelectListener {

    /** 结束选择，滑动停止时回调 */
    void endSelect(int id, String text);

    /** 选中的内容，滑动的过程中会不断回调 */
    void selecting(int id, String text);
  }

  public interface onInputListener {

    /** 输入的内容，输入完成后，按回车键时回调 */
    void endInput(WheelView wheelView, EditText editText, String text);

    /**
     * 开始输入时回调
     *
     * @param editText 输入框控件
     * @param selected 已选内容
     */
    void onStartInput(WheelView wheelView, EditText editText, String selected);
  }
}
