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

public class MainActivity extends Activity {
    private static final String ROBOFORM_PACKAGE = "com.siber.roboform";
    private static final String FIXED_RECIPIENT = "2137765821";

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
        root.addView(title, matchWrap(dp(10)));

        TextView recipient = new TextView(this);
        recipient.setText("固定收款人：" + FIXED_RECIPIENT);
        recipient.setTextSize(16);
        recipient.setTextColor(Color.WHITE);
        recipient.setGravity(Gravity.CENTER);
        root.addView(recipient, matchWrap(dp(6)));

        TextView currency = new TextView(this);
        currency.setText("固定币种：USD 美元");
        currency.setTextSize(14);
        currency.setTextColor(0xFFBDBDBD);
        currency.setGravity(Gravity.CENTER);
        root.addView(currency, matchWrap(dp(18)));

        amountInput = new EditText(this);
        amountInput.setHint("金额，例如 25.00");
        amountInput.setSingleLine(true);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, matchWrap(dp(16)));

        Button payButton = new Button(this);
        payButton.setText("付款（RoboForm）");
        payButton.setAllCaps(false);
        payButton.setOnClickListener(v -> openPayPalWithRoboForm());
        root.addView(payButton, matchWrap(dp(12)));

        TextView note = new TextView(this);
        note.setText("收款人锁定为 2137765821。PayPal 页面只交给手表上的 RoboForm 打开；密码由 RoboForm 管理，本应用不读取或保存 PayPal 密码。");
        note.setTextSize(12);
        note.setTextColor(0xFF9E9E9E);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(0));

        setContentView(scroll);
    }

    private void openPayPalWithRoboForm() {
        String amountRaw = amountInput.getText().toString().trim();

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
        Uri uri = Uri.parse("https://www.paypal.me/" + FIXED_RECIPIENT + "/" + Uri.encode(amountText + "USD"));

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(ROBOFORM_PACKAGE);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("请先在手表上安装最新版 RoboForm");
        } catch (Exception e) {
            toast("无法用 RoboForm 打开 PayPal");
        }
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
