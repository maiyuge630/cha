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
    static final String EXTRA_OPEN_WITHDRAW = "open_withdraw";
    static final String PAYPAL_WITHDRAW_URL = "https://www.paypal.com/myaccount/money/balances/withdraw";
    private static final int REQUEST_NOTIFICATIONS = 10;
    private static final String SAMSUNG_INTERNET_PACKAGE = "com.sec.android.app.sbrowser";

    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_OPEN_WITHDRAW, false)) {
            openPayPalWithdraw();
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
        title.setText("PayPal 收款 → BOA");
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

        Button withdrawButton = new Button(this);
        withdrawButton.setText("打开 PayPal 提现到 BOA");
        withdrawButton.setAllCaps(false);
        withdrawButton.setOnClickListener(v -> openPayPalWithdraw());
        root.addView(withdrawButton, matchWrap(dp(14)));

        TextView note = new TextView(this);
        note.setText("手机收到 PayPal 收款通知后，手表会提示“收到钱 · 点此提现到 BOA”。点击后直接打开 PayPal 官方提现入口。BOA 需要提前在 PayPal 里关联；银行选择、金额、标准/即时转账和最终确认仍由 PayPal 官方页面完成。");
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
                ? "手表通知：已允许 ✓\n等待 PayPal 收款提醒"
                : "手表通知：未允许\n请点下面按钮，只需设置一次");
    }

    private void openPayPalWithdraw() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_WITHDRAW_URL));
        intent.setPackage(SAMSUNG_INTERNET_PACKAGE);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("请先在手表上安装 Samsung Internet");
        } catch (Exception e) {
            toast("无法打开 PayPal 提现页面");
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
