package com.pl.wheelview;

/**
 * Created by penglu on 2015/11/25.
 */
public enum WeekDay {
    Sun("sunday","周日"){
        public WeekDay getNextDay(){
            return Mon;
        }
    },
    Mon("monday","周一"){
        public WeekDay getNextDay(){
            return Tues;
        }
    },
    Tues("tuesday","周二"){
        public WeekDay getNextDay(){
            return Wed;
        }
    },
    Wed("wednesday","周三"){
        public WeekDay getNextDay(){
            return Thur;
        }
    },
    Thur("thursday","周四"){
        public WeekDay getNextDay(){
            return Fri;
        }
    },
    Fri("friday","周五"){
        public WeekDay getNextDay(){
            return Sat;
        }
    },
    Sat("saturday","周六"){
        public WeekDay getNextDay(){
            return Sun;
        }
    },
    NULL("null","仅一次");
    private String weekDay;
    private String weekDayCH;
    WeekDay(String weekDay, String weekDayCH){
        this.weekDay=weekDay;
        this.weekDayCH=weekDayCH;
    }

    @Override
    public String toString() {
        return weekDay;
    }


    public String getWeekDayCH(){
        return weekDayCH;
    }
    public WeekDay getNextDay(){
        return NULL;
    }


}
