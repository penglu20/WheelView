# WheelView

实现转轮的选择功能，效果见下图:

![效果图](https://raw.githubusercontent.com/l465659833/WheelView/master/art/sample.gif)

本项目是由[这个项目](https://github.com/helloJp/WheelView)修改而成，由于我做的改动实在太大了，基本上除了原来的大体框架以外，内部的实现逻辑全都做了大量修改，所以我另开了一个项目，但必须感谢原作者给我的启发。

先说说我所做的优化和改善吧：

1. 滑动更加流畅顺滑；
2. 可以点击实现单步的增减；
3. 滑动过程中触摸立即停止滑动；
4. 滑动的距离跟滑动的速度成正比，和滑动的时间成反比；
5. 实现循环滚动，并可以切换

# How To Use

add to your build.gradle files:

```
dependencies {
    compile 'com.pl:wheelview:0.7.2'
}
```


以下内容来自[原项目](https://github.com/helloJp/WheelView)，毕竟连代码都copy了，这些细节也懒得在乎了

# Attributes


| attr 属性          | description 描述 |
|:---				 |:---|
| lineColor  	     | divider line color 分割线颜色 |
| lineHeight  	     | divider line height 分割线高度 |
| itemNumber	 	 | wheelview show item count 此wheelView显示item的个数 |
| noEmpty 			 | if set true select area can't be null(empty),or could be empty 设置true则选中不能为空，否则可以是空 |
| normalTextColor 	 | unSelected Text color 未选中文本颜色 |
| normalTextSize 	 | unSelected Text size 未选中文本字体大小 |
| selectedTextColor | selected Text color 选中文本颜色 |
| selectedTextSize 	 | selected Text size 选中文本字体大小 |
| unitHeight 		 | item unit height 每个item单元的高度 |
| isCyclic 		     | if scroll cyclic 是否循环滚动 |
| maskDarkColor 		     | color of mask far from the select end 离选中位置远的遮罩颜色 |
| maskLightColor 		     |  color of mask near the select end 离选中位置近的遮罩颜色 |

# Method
### 1. setData(ArrayList<String> data)
set WheelView data</br> 
设置WheelView的数据

### 2. refreshData(ArrayList<String> data) 
**refresh** WheelView data ,and draw again</br>
**刷新** WheelView的数据，并重绘

### 3. int getSelected()
get selected item index</br>
获取选中项的index

### 4. String getSelectedText()
get selected item text</br>
获取选中项的文本信息

### 5. boolean isScrolling
is WheelView is scrolling</br>
获取WheelView是否在滚动中

### 6. boolean isEnable()
is WheelView is enable</br>
获取wheelView是否可用

### 7. void setEnable(boolean isEnable)  
set WheelView enable</br>
设置WheelView是否可用

### 8. void setDefault(int index)
set **default selected index**</br>
设置**默认选中项的index**
 
### 9. int getListSize() 
get WheelView item count</br>
获取WheelView的item项个数

### 10. String getItemText(int index)
get the text by index </br>
获取index位置上的文本数据

### 11. void setOnSelectListener(OnSelectListener onSelectListener)
set listener on WheelView that can get info when WheelView is **scrolling** or **stop scroll**.</br>
对WheelView**设置监听**，在 **滑动过程** 或者 **滑动停止** 返回数据信息。

### 12. void setItemNumber(int itemNumber)
set the number ofitem show in WheelView</br>
设置WheelView展示的项目数量

### 13. int getItemNumber()
get the number ofitem show in WheelView</br>
获取WheelView展示的项目数量

### 14. void setCyclic(boolean cyclic)
set if scroll cyclic
设置是否循环滚动

### 15. boolean isCyclic()
get if scroll cyclic
获取是否循环滚动



具体的一些构思和设计思路，参见[这篇文章](http://www.jianshu.com/p/4b3e2373d0e2)和[这篇文章](http://www.jianshu.com/p/c01c1dda5a8a)
