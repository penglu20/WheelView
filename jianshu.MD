在[【造轮子系列】转轮选择工具——WheelView](http://www.jianshu.com/p/4b3e2373d0e2)中，我详细记录了这个自定义控件的设计思路和相关数据的计算。由于本人能力有限，当时还留下了一些不足的地方，主要包括：

1. 滑动的性能和流畅性有待提高，特别是快速滑动时的效果
2. 没有实现循环滚动的效果

经过这一段时间的不断改进，现在基本上已经比较完美了，接近ios闹钟的滚轮时间选择器的效果了。下面结合代码，对比之前的版本，记录一下我做的这些改进。

###效果图

![效果图](https://raw.githubusercontent.com/l465659833/WheelView/master/art/sample.gif)

###源码
[WheelView](https://github.com/l465659833/WheelView)

###核心计算思想的转变
性能优化说白了就是在得到相同结果的前提下进行最少的计算。在滚动的过程中，最大的计算量就是计算每一个item的位置，再根据位置来判断每个item是否要进行绘制、如何绘制。

这部分计算，原先是通过遍历所有的item来设定位置的。但是，所有item的相对位置是固定的，所以只要判断了一个item的位置，其他item的位置也就可以得到了。这里是将比较耗时的乘除运算用简单的加减运算代替，可以提高几倍的计算性能。

更进一步，设置toShowItems来记录将会被显示出来的item。先计算第一个item的位置，然后根据每个item的高度就可以得到可能显示的item，将这些item记录在toShowItems中。在实际绘制的时候，只需要对toShowItems进行遍历计算就行了,这样可以将时间复杂度由原来的O(n)变成O(1)，计算效率大幅度提升(其中n代表item的个数)。

利用toShowItems以及相对位置的方法，还可以很方便的调整item显示的位置，从而实现循环滚动的效果。

**相关代码如下**
```java
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
	 * y坐标,代表绝对位置，由id和unitHeight决定
	 */
	int y = 0;
	/**
	 * 移动距离，代表滑动的相对位置，用以调整当前位置
	 */
	int move = 0;
}
private int moveDistance;//所有item的移动距离，用同一个变量记录，减少计算
private ItemObject[] toShowItems;//其长度等于itemNumber+2
private void findItemsToShow(){
	if (_isCyclic) {
		//循环模式下，将moveDistance限定在一定的范围内循环变化，同时要保证滚动的连续性
		if (moveDistance > unitHeight * itemList.size()) {
			moveDistance = moveDistance % ((int) unitHeight * itemList.size());
		} else if (moveDistance < 0) {
			moveDistance = moveDistance % ((int) unitHeight * itemList.size()) + (int) unitHeight * itemList.size();
		}
		int move = moveDistance;
		ItemObject first = itemList.get(0);
		int firstY = first.y + move;
		int firstNumber = (int) (Math.abs(firstY / unitHeight));//滚轮中显示的第一个item的index
		int restMove = (int) (firstY - unitHeight * firstNumber);//用以保证滚动的连续性
		int takeNumberStart = firstNumber;
		synchronized (toShowItems) {
			for (int i = 0; i < toShowItems.length; i++) {
				int takeNumber = takeNumberStart + i;
				int realNumber = takeNumber;
				if (takeNumber < 0) {
					realNumber = itemList.size() + takeNumber;//调整循环滚动显示的index
				} else if (takeNumber >= itemList.size()) {
					realNumber = takeNumber - itemList.size();//调整循环滚动显示的index
				}
				toShowItems[i] = itemList.get(realNumber);
				toShowItems[i].move((int) (unitHeight * ((i - realNumber)%itemList.size())) - restMove);//设置滚动的相对位置
			}
		}
	}else {
		//非循环模式下，滚动到边缘即停止动画
		if (moveDistance > unitHeight * itemList.size()-itemNumber/2*unitHeight-unitHeight) {
			moveDistance = (int)( unitHeight * itemList.size()-itemNumber/2*unitHeight-unitHeight);
			moveHandler.removeMessages(GO_ON_MOVE_REFRESH);
			moveHandler.sendEmptyMessage(GO_ON_MOVE_INTERRUPTED);
		} else if (moveDistance < -itemNumber/2*unitHeight) {
			moveDistance = (int) (-itemNumber/2*unitHeight);
			moveHandler.removeMessages(GO_ON_MOVE_REFRESH);
			moveHandler.sendEmptyMessage(GO_ON_MOVE_INTERRUPTED);
		}

		int move = moveDistance;
		ItemObject first = itemList.get(0);

		int firstY = first.y + move;
		int firstNumber = (int) (firstY / unitHeight);//滚轮中显示的第一个item的index
		int restMove = (int) (firstY - unitHeight * firstNumber);//用以保证滚动的连续性
		int takeNumberStart = firstNumber ;
		synchronized (toShowItems) {
			for (int i = 0; i < toShowItems.length; i++) {
				int takeNumber = takeNumberStart + i;
				int realNumber = takeNumber;
				if (takeNumber < 0) {
					realNumber = -1;//用以标识超出的部分
				} else if (takeNumber >= itemList.size()) {
					realNumber = -1;//用以标识超出的部分
				}
				if (realNumber==-1){
					toShowItems[i]=null;//设置为null，则会留出空白
				}else {
					toShowItems[i] = itemList.get(realNumber);
					toShowItems[i].move((int) (unitHeight * (i - realNumber)) - restMove);//设置滚动的相对位置
				}
			}
		}

	}
    //调用回调
	if (onSelectListener!=null&&toShowItems[itemNumber/2]!=null){
		callbackHandler.post(new Runnable() {
			@Override
			public void run() {
				onSelectListener.selecting(toShowItems[itemNumber/2].id,toShowItems[itemNumber/2].getItemText());
			}
		});
	}

}

```

除了上面的代码，goonMove(),noEmpty(),slowMove()等函数中都有一些结合toShowItems的修改，可以在[源码](https://github.com/l465659833/WheelView)查看。

###使用VelocityTracker来计算滑动速度

原来实现滚轮的滚动效果时，我是使用了手指按下到抬起来所划过的距离除以划过这一段距离所用的时间来计算滑动的速度的，然后再根据速度来判断是否要进行快速的连续滚动。这样做有一些不足之处，包括：
* 如果先按下一段时间再快速滑动，则由于时间过长，导致计算得到的滑动速度很小
* 对先向下滑动再向上快速滑动，会判断成滑动距离很短，导致计算得到的滑动速度很小
* 对于滑动距离极短，滑动时间也极短的情况，难以计算出合理的速度值

由于这些原因，导致滑动的效果并不是特别好，不过VelocityTracker完美的解决了这些问题，直接看代码。

**VelocityTracker的用法**
```java
@Override
public boolean onTouchEvent(MotionEvent event) {
	if (mVelocityTracker == null) {
		mVelocityTracker = VelocityTracker.obtain();
	}
	mVelocityTracker.addMovement(event);

	int y = (int) event.getY();
	switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		//处理动作
			 break;              
		case MotionEvent.ACTION_MOVE:
		//处理动作
			 break;              
		case MotionEvent.ACTION_UP:             

			//用速度来判断是非快速滑动
			VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
			int initialVelocity = (int) velocityTracker.getYVelocity();
			if (Math.abs(initialVelocity)>mMinimumFlingVelocity) {
				goonMove(initialVelocity,y - downY);
			} else {
				//处理其他动作				
			}

			mVelocityTracker.recycle();
			mVelocityTracker = null;
			break;
		default:
			break;
	}
	return true;
}
```

###一些坑
toShowItems和VelocityTracker结合使用，就是对WheelView进行改进的主要的部分，在这过程中也遇到了一些坑：

1. 待选项数量少于itemNumber的情况下，如果使用循环滚动，则会造成不良效果，所以这种情况下强制关闭循环滚动；
2. findItemsToShow()函数调用的时机，是在每一次重绘以前，也就是postInvalidate()或者invalidate()前调用，这样才能保证每次绘制的是最新的位置；
3. 用setDefault()设置默认选项的时候，会计算从当前选项滚动到目标选项的距离，
如果直接使用itemList.get(index).moveToSelected()计算可能会导致距离计算错误，
因为findItemsToShow()只判断当前可能显示的item，并设置move，而不会将其他item的move置为0，从而可能影响判断。
所以必须先将itemList中的move全设置为零，再计算距离。代码如下：

```java
    public void setDefault(int index) {
        defaultIndex=index;
        if (index > itemList.size() - 1)
            return;
        moveDistance=0;
        for (ItemObject item :itemList){
            item.move=0;
        }
        findItemsToShow();
        float move = itemList.get(index).moveToSelected();
        defaultMove((int) move);
    }
```


###总结
性能优化和动画效果优化是一个不断尝试和调优的过程，本文所述可能在不久以后就会被推翻重来。

如果你想使用这个WheelView，可以从github得到[源码](https://github.com/l465659833/WheelView)
或者直接在build.gradle中添加依赖使用

```
dependencies {
    compile 'com.pl:wheelview:0.6.4'
}
```
