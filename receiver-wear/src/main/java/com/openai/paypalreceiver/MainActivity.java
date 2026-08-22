package com.openai.paypalreceiver;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String EXTRA_OPEN_PAYPAL = "open_paypal";
    private static final int REQUEST_NOTIFICATIONS = 10;
    private static final String SAMSUNG_INTERNET_PACKAGE = "com.sec.android.app.sbrowser";
    private static final String PAYPAL_ACTIVITY_URL = "https://www.paypal.com/myaccount/activities/";

    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_OPEN_PAYPAL, false)) {
            openPayPalActivity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        ReceiveNotifier.flushPending(this);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(22);
        root.setPadding(pad, dp(28), pad, dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("PayPal 收款提醒");
        title.setTextSize(21);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(12)));

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setTextColor(Color.WHITE);
        statusView.setGravity(Gravity.CENTER);
        root.addView(statusView, matchWrap(dp(16)));

        Button permissionButton = new Button(this);
        permissionButton.setText("允许手表通知");
        permissionButton.setAllCaps(false);
        permissionButton.setOnClickListener(v -> requestNotificationPermission());
        root.addView(permissionButton, matchWrap(dp(10)));

        Button activityButton = new Button(this);
        activityButton.setText("打开 PayPal 交易记录");
        activityButton.setAllCaps(false);
        activityButton.setOnClickListener(v -> openPayPalActivity());
        root.addView(activityButton, matchWrap(dp(14)));

        TextView note = new TextView(this);
        note.setText("手机收到 PayPal 收款通知后，这里会自动弹提醒。点提醒会在手表 Samsung Internet 打开 PayPal 交易记录；真正的接受/确认仍在 PayPal 官方页面完成。");
        note.setTextSize(12);
        note.setTextColor(0xFFBDBDBD);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(0));

        setContentView(scroll);
        refreshStatus();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else {
            toast("手表通知已经允许");
        }
    }

    private void refreshStatus() {
        boolean allowed = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        statusView.setText(allowed
                ? "手表通知：已允许 ✓\n等待手机端 PayPal 收款提醒"
                : "手表通知：未允许\n请点下面按钮，只需设置一次");
    }

    private void openPayPalActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_ACTIVITY_URL));
        intent.setPackage(SAMSUNG_INTERNET_PACKAGE);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("请先在手表上安装 Samsung Internet");
        } catch (Exception e) {
            toast("无法打开 PayPal 交易记录");
        }
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
