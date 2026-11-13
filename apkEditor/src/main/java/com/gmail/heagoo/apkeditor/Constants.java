package com.gmail.heagoo.apkeditor;

class Constants {

    public interface ACTION {
        public static final String MAIN_ACTION = "com.gmail.heagoo.action.apkcompose";
        public static final String STARTFOREGROUND_ACTION = "com.gmail.heagoo.action.startforeground";
        public static final String STOPFOREGROUND_ACTION = "com.gmail.heagoo.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static final int FOREGROUND_SERVICE = 8001;
        public static final int BUILD_STAGE = 8002;
    }

    Constants() {
    }
}
