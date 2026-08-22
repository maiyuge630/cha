package com.openai.paypalwearshortcut;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;

public class MainActivity extends Activity {
    private static final String SAMSUNG_INTERNET_PACKAGE = "com.sec.android.app.sbrowser";
    private static final String FIXED_RECIPIENT = "2137765821";

    private EditText passwordInput;
    private EditText amountInput;
    private TextView passwordStatus;
    private Button deletePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SecureClipboard.clearExpiredIfOwned(this);
        refreshPasswordStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(22);
        root.setPadding(pad, dp(26), pad, dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("PayPal 转账");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(8)));

        TextView fixed = new TextView(this);
        fixed.setText("固定收款人：" + FIXED_RECIPIENT + "\n固定币种：USD 美元");
        fixed.setTextSize(14);
        fixed.setTextColor(0xFFBDBDBD);
        fixed.setGravity(Gravity.CENTER);
        root.addView(fixed, matchWrap(dp(16)));

        passwordStatus = new TextView(this);
        passwordStatus.setTextSize(13);
        passwordStatus.setTextColor(Color.WHITE);
        passwordStatus.setGravity(Gravity.CENTER);
        root.addView(passwordStatus, matchWrap(dp(8)));

        passwordInput = new EditText(this);
        passwordInput.setHint("PayPal 密码（只需保存一次）");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput, matchWrap(dp(8)));

        Button savePasswordButton = new Button(this);
        savePasswordButton.setText("保存 / 更新密码");
        savePasswordButton.setAllCaps(false);
        savePasswordButton.setOnClickListener(v -> savePassword());
        root.addView(savePasswordButton, matchWrap(dp(6)));

        deletePasswordButton = new Button(this);
        deletePasswordButton.setText("删除已保存密码");
        deletePasswordButton.setAllCaps(false);
        deletePasswordButton.setOnClickListener(v -> deletePassword());
        root.addView(deletePasswordButton, matchWrap(dp(18)));

        amountInput = new EditText(this);
        amountInput.setHint("金额，例如 25.00");
        amountInput.setSingleLine(true);
        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, matchWrap(dp(10)));

        Button payButton = new Button(this);
        payButton.setText("付款");
        payButton.setAllCaps(false);
        payButton.setOnClickListener(v -> openPayPalOnWatch());
        root.addView(payButton, matchWrap(dp(12)));

        TextView note = new TextView(this);
        note.setText("有已保存密码时，点付款会把密码临时放进系统剪贴板约 20 秒，然后在手表 Samsung Browser 打开 PayPal。到密码框点“粘贴”即可。密码用 Android Keystore 加密保存在本机，本 App 不申请网络权限；登录和最终付款确认仍由 PayPal 官方页面完成。");
        note.setTextSize(11);
        note.setTextColor(0xFF9E9E9E);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(0));

        setContentView(scroll);
        refreshPasswordStatus();
    }

    private void savePassword() {
        String password = passwordInput.getText().toString();
        if (password.isEmpty()) {
            toast("请输入 PayPal 密码");
            return;
        }
        try {
            PasswordVault.save(this, password);
            passwordInput.setText("");
            refreshPasswordStatus();
            toast("密码已加密保存在这块手表");
        } catch (Exception e) {
            toast("密码保存失败");
        }
    }

    private void deletePassword() {
        PasswordVault.delete(this);
        SecureClipboard.clearIfOwned(this);
        passwordInput.setText("");
        refreshPasswordStatus();
        toast("已删除本机保存的密码");
    }

    private void refreshPasswordStatus() {
        if (passwordStatus == null) return;
        boolean saved = PasswordVault.hasPassword(this);
        passwordStatus.setText(saved ? "登录密码：已安全保存" : "登录密码：尚未保存");
        if (deletePasswordButton != null) {
            deletePasswordButton.setVisibility(saved ? View.VISIBLE : View.GONE);
        }
    }

    private void openPayPalOnWatch() {
        final BigDecimal amount;
        try {
            amount = PaymentLink.parseAmount(amountInput.getText().toString());
        } catch (Exception e) {
            toast("请输入正确的美元金额");
            return;
        }

        Uri uri = Uri.parse(PaymentLink.buildPayPalMeUrl(FIXED_RECIPIENT, amount));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(SAMSUNG_INTERNET_PACKAGE);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        if (intent.resolveActivity(getPackageManager()) == null) {
            toast("请先在手表上安装 Samsung Browser");
            return;
        }

        boolean copiedPassword = false;
        if (PasswordVault.hasPassword(this)) {
            String password = PasswordVault.get(this);
            if (!password.isEmpty()) {
                copiedPassword = SecureClipboard.copyTemporary(this, password);
            }
        }

        if (copiedPassword) {
            toast("密码已临时复制 20 秒，到 PayPal 密码框点“粘贴”");
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            if (copiedPassword) SecureClipboard.clearIfOwned(this);
            toast("请先在手表上安装 Samsung Browser");
        } catch (Exception e) {
            if (copiedPassword) SecureClipboard.clearIfOwned(this);
            toast("无法在手表浏览器打开 PayPal");
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
