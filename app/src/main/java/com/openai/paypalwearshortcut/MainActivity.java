package com.openai.paypalwearshortcut;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.List;

public class MainActivity extends Activity {
    private static final String SAMSUNG_INTERNET_PACKAGE = "com.sec.android.app.sbrowser";

    private EditText recipientInput;
    private EditText amountInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        root.setPadding(pad, dp(28), pad, dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("PayPal 转账");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(8)));

        TextView currency = new TextView(this);
        currency.setText("固定币种：USD 美元");
        currency.setTextSize(14);
        currency.setTextColor(0xFFBDBDBD);
        currency.setGravity(Gravity.CENTER);
        root.addView(currency, matchWrap(dp(18)));

        recipientInput = new EditText(this);
        recipientInput.setHint("PayPal.Me 用户名 / 链接");
        recipientInput.setSingleLine(true);
        recipientInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(recipientInput, matchWrap(dp(10)));

        amountInput = new EditText(this);
        amountInput.setHint("金额，例如 25.00");
        amountInput.setSingleLine(true);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, matchWrap(dp(16)));

        Button payButton = new Button(this);
        payButton.setText("仅在手表上付款");
        payButton.setAllCaps(false);
        payButton.setOnClickListener(v -> openPayPalOnWatch());
        root.addView(payButton, matchWrap(dp(12)));

        TextView note = new TextView(this);
        note.setText("只会调用手表本机的 Samsung Internet。不会把付款页面发送到手机。登录和最终确认仍由 PayPal 官方页面完成。");
        note.setTextSize(12);
        note.setTextColor(0xFF9E9E9E);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(0));

        setContentView(scroll);
    }

    private void openPayPalOnWatch() {
        String recipient = normalizeRecipient(recipientInput.getText().toString());
        String amountRaw = amountInput.getText().toString().trim();

        if (recipient == null || !recipient.matches("[A-Za-z0-9]{1,20}")) {
            toast("请输入正确的 PayPal.Me 用户名或链接");
            return;
        }

        final BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            toast("请输入正确的美元金额");
            return;
        }

        String amountText = amount.stripTrailingZeros().toPlainString();
        Uri uri = Uri.parse("https://www.paypal.me/" + Uri.encode(recipient) + "/" + Uri.encode(amountText + "USD"));

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(SAMSUNG_INTERNET_PACKAGE);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("请先在手表上安装 Samsung Internet");
        } catch (Exception e) {
            toast("无法在手表浏览器打开 PayPal");
        }
    }

    private String normalizeRecipient(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;

        while (value.startsWith("@")) {
            value = value.substring(1).trim();
        }

        String lower = value.toLowerCase();
        if (lower.startsWith("paypal.me/") || lower.startsWith("www.paypal.me/")) {
            value = "https://" + value;
            lower = value.toLowerCase();
        }

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            try {
                Uri parsed = Uri.parse(value);
                String host = parsed.getHost();
                if (host == null || !(host.equalsIgnoreCase("paypal.me") || host.equalsIgnoreCase("www.paypal.me"))) {
                    return null;
                }
                List<String> segments = parsed.getPathSegments();
                if (segments.isEmpty()) return null;
                value = segments.get(0);
            } catch (Exception e) {
                return null;
            }
        }

        return value.trim();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
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
