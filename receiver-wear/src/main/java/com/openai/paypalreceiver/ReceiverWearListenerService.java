package com.openai.paypalreceiver;

import android.net.Uri;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class ReceiverWearListenerService extends WearableListenerService {
    private static final String PATH_PREFIX = "/paypal_receive/";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() != DataEvent.TYPE_CHANGED) continue;
            Uri uri = event.getDataItem().getUri();
            if (uri == null || uri.getPath() == null || !uri.getPath().startsWith(PATH_PREFIX)) continue;

            DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            String title = map.getString("title", "PayPal 收款");
            String text = map.getString("text", "收到一条 PayPal 收款通知");
            long postedAt = map.getLong("time", System.currentTimeMillis());

            ReceiveNotifier.showOrStore(this, title, text, postedAt);
            Wearable.getDataClient(this).deleteDataItems(uri);
        }
    }
}
