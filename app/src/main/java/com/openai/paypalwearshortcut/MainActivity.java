package com.openai.paypalwearshortcut;

import android.app.Activity;
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

public class MainActivity extends Activity {
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
        recipientInput.setHint("PayPal.Me 用户名");
        recipientInput.setSingleLine(true);
        recipientInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(recipientInput, matchWrap(dp(10)));

        amountInput = new EditText(this);
        amountInput.setHint("金额，例如 25.00");
        amountInput.setSingleLine(true);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, matchWrap(dp(16)));

        Button payButton = new Button(this);
        payButton.setText("用 PayPal 继续");
        payButton.setAllCaps(false);
        payButton.setOnClickListener(v -> openPayPal());
        root.addView(payButton, matchWrap(dp(12)));

        TextView note = new TextView(this);
        note.setText("登录和最终付款确认均在 PayPal 官方页面完成。本应用不保存密码或银行卡信息。");
        note.setTextSize(12);
        note.setTextColor(0xFF9E9E9E);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(0));

        setContentView(scroll);
    }

    private void openPayPal() {
        String recipient = recipientInput.getText().toString().trim();
        String amountRaw = amountInput.getText().toString().trim();

        if (recipient.isEmpty()) {
            toast("请输入 PayPal.Me 用户名");
            return;
        }
        if (!recipient.matches("[A-Za-z0-9._-]+")) {
            toast("用户名格式不正确");
            return;
        }

        final BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            toast("请输入正确的美元金额");
            return;
        }

        String amountText = amount.stripTrailingZeros().toPlainString();
        Uri uri = Uri.parse("https://www.paypal.me/" + Uri.encode(recipient) + "/" + Uri.encode(amountText + "USD"));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        try {
            startActivity(intent);
        } catch (Exception e) {
            toast("手表上没有可打开网页的应用");
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
