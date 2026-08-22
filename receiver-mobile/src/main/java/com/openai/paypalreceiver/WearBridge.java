package com.openai.paypalreceiver;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

final class WearBridge {
    static final String PATH_PREFIX = "/paypal_receive/";
    static final String KEY_TITLE = "title";
    static final String KEY_TEXT = "text";
    static final String KEY_TIME = "time";
    static final String KEY_SOURCE = "source";

    private WearBridge() {}

    static Task<DataItem> sendReceiveAlert(Context context, String sourceKey, String title, String text, long postedAt) {
        String suffix = Integer.toHexString((sourceKey == null ? "" : sourceKey).hashCode()) + "_" + postedAt;
        PutDataMapRequest mapRequest = PutDataMapRequest.create(PATH_PREFIX + suffix);
        mapRequest.getDataMap().putString(KEY_TITLE, title == null ? "PayPal 收款" : title);
        mapRequest.getDataMap().putString(KEY_TEXT, text == null ? "收到一条 PayPal 收款通知" : text);
        mapRequest.getDataMap().putLong(KEY_TIME, postedAt);
        mapRequest.getDataMap().putString(KEY_SOURCE, "paypal_android");
        PutDataRequest request = mapRequest.asPutDataRequest().setUrgent();
        return Wearable.getDataClient(context.getApplicationContext()).putDataItem(request);
    }
}
