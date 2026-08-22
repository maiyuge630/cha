package com.openai.paypalreceiver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        root.setPadding(pad, dp(32), pad, dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("PayPal 收款 → BOA · 手机端");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(14)));

        statusView = new TextView(this);
        statusView.setTextSize(15);
        statusView.setTextColor(Color.WHITE);
        statusView.setGravity(Gravity.CENTER);
        root.addView(statusView, matchWrap(dp(18)));

        Button accessButton = new Button(this);
        accessButton.setText("开启通知访问");
        accessButton.setAllCaps(false);
        accessButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception e) {
                toast("无法打开通知访问设置");
            }
        });
        root.addView(accessButton, matchWrap(dp(10)));

        Button testButton = new Button(this);
        testButton.setText("发送测试收款提醒到手表");
        testButton.setAllCaps(false);
        testButton.setOnClickListener(v -> sendTest());
        root.addView(testButton, matchWrap(dp(16)));

        TextView note = new TextView(this);
        note.setText("只监听官方 PayPal Android App 的收款类通知，并通过 Wear OS Data Layer 发给配对手表。手表收到提醒后可直接进入 PayPal 官方提现页面；BOA 需要提前在 PayPal 中关联。不会读取 PayPal 或 BOA 密码，也不会自动确认银行转账。");
        note.setTextSize(13);
        note.setTextColor(0xFFBDBDBD);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(0));

        setContentView(scroll);
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        String enabledListeners = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        boolean enabled = enabledListeners != null && enabledListeners.contains(getPackageName());
        statusView.setText(enabled
                ? "通知访问：已开启 ✓"
                : "通知访问：未开启\n请点下面按钮，只需设置一次");
    }

    private void sendTest() {
        long now = System.currentTimeMillis();
        WearBridge.sendReceiveAlert(this, "manual_test_" + now,
                        "PayPal 测试收款", "测试：收到 $1.00", now)
                .addOnSuccessListener(item -> toast("测试提醒已交给 Wear OS 同步"))
                .addOnFailureListener(error -> toast("发送失败：请确认手机已与手表配对"));
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = bottomMargin;
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
