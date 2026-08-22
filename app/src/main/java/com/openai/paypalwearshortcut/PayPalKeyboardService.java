package com.openai.paypalwearshortcut;

import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PayPalKeyboardService extends InputMethodService {

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = dp(10);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this);
        title.setText("PayPal Fill");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(6)));

        TextView note = new TextView(this);
        note.setText("只在你从 PayPal USD 打开付款页后使用");
        note.setTextColor(0xFFBDBDBD);
        note.setTextSize(10);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(dp(8)));

        Button accountButton = new Button(this);
        accountButton.setText("填入账号");
        accountButton.setAllCaps(false);
        accountButton.setOnClickListener(v -> commitStored(false));
        root.addView(accountButton, matchWrap(dp(6)));

        Button passwordButton = new Button(this);
        passwordButton.setText("填入密码");
        passwordButton.setAllCaps(false);
        passwordButton.setOnClickListener(v -> commitStored(true));
        root.addView(passwordButton, matchWrap(dp(6)));

        Button nextButton = new Button(this);
        nextButton.setText("切换回其他键盘");
        nextButton.setAllCaps(false);
        nextButton.setOnClickListener(v -> {
            try {
                if (!switchToNextInputMethod(false)) {
                    Toast.makeText(this, "请用系统输入法选择器切换键盘", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "请用系统输入法选择器切换键盘", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(nextButton, matchWrap(0));

        return root;
    }

    private void commitStored(boolean password) {
        if (!CredentialStore.isRecentExpectedPayPalLaunch(this)) {
            Toast.makeText(this, "请先从 PayPal USD 点付款，再回来填入", Toast.LENGTH_LONG).show();
            return;
        }
        if (!CredentialStore.hasCredential(this)) {
            Toast.makeText(this, "请先在 PayPal USD 里保存账号和密码", Toast.LENGTH_LONG).show();
            return;
        }

        String value = password ? CredentialStore.getPassword(this) : CredentialStore.getAccount(this);
        if (value == null || value.isEmpty()) {
            Toast.makeText(this, "没有可用的已保存内容", Toast.LENGTH_LONG).show();
            return;
        }

        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            Toast.makeText(this, "当前没有可输入的文本框", Toast.LENGTH_LONG).show();
            return;
        }

        connection.commitText(value, 1);
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
