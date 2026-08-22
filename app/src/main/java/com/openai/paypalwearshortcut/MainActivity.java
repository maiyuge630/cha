package com.openai.paypalwearshortcut;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

    private EditText accountInput;
    private EditText passwordInput;
    private EditText amountInput;
    private TextView savedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(22);
        root.setPadding(pad, dp(24), pad, dp(34));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("PayPal 转账");
        title.setTextSize(21);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(8)));

        TextView fixed = new TextView(this);
        fixed.setText("收款人：" + FIXED_RECIPIENT + "   ·   USD");
        fixed.setTextSize(14);
        fixed.setTextColor(0xFFBDBDBD);
        fixed.setGravity(Gravity.CENTER);
        root.addView(fixed, matchWrap(dp(16)));

        savedStatus = new TextView(this);
        savedStatus.setTextSize(13);
        savedStatus.setTextColor(0xFFBDBDBD);
        savedStatus.setGravity(Gravity.CENTER);
        root.addView(savedStatus, matchWrap(dp(10)));

        accountInput = new EditText(this);
        accountInput.setHint("PayPal 账号 / 邮箱");
        accountInput.setSingleLine(true);
        accountInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        root.addView(accountInput, matchWrap(dp(8)));

        passwordInput = new EditText(this);
        passwordInput.setHint("PayPal 密码（仅本机加密保存）");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput, matchWrap(dp(8)));

        Button saveButton = new Button(this);
        saveButton.setText("保存 / 更新登录");
        saveButton.setAllCaps(false);
        saveButton.setOnClickListener(v -> saveCredential());
        root.addView(saveButton, matchWrap(dp(8)));

        Button enableKeyboardButton = new Button(this);
        enableKeyboardButton.setText("1. 启用 PayPal 填充键盘");
        enableKeyboardButton.setAllCaps(false);
        enableKeyboardButton.setOnClickListener(v -> openKeyboardSettings());
        root.addView(enableKeyboardButton, matchWrap(dp(8)));

        Button chooseKeyboardButton = new Button(this);
        chooseKeyboardButton.setText("2. 选择 PayPal 填充键盘");
        chooseKeyboardButton.setAllCaps(false);
        chooseKeyboardButton.setOnClickListener(v -> showKeyboardPicker());
        root.addView(chooseKeyboardButton, matchWrap(dp(8)));

        Button deleteButton = new Button(this);
        deleteButton.setText("删除已保存登录");
        deleteButton.setAllCaps(false);
        deleteButton.setOnClickListener(v -> deleteCredential());
        root.addView(deleteButton, matchWrap(dp(18)));

        amountInput = new EditText(this);
        amountInput.setHint("付款金额，例如 25.00");
        amountInput.setSingleLine(true);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, matchWrap(dp(10)));

        Button payButton = new Button(this);
        payButton.setText("付款（Samsung Browser）");
        payButton.setAllCaps(false);
        payButton.setOnClickListener(v -> openPayPalOnWatch());
        root.addView(payButton, matchWrap(dp(12)));

        TextView note = new TextView(this);
        note.setText("Samsung Browser 在这块手表上没有调用 Android Autofill，所以这一版改用本机输入法。打开 PayPal 后点登录框，选择 PayPal Fill Keyboard，再点“填入账号”或“填入密码”。密码由 Android Keystore + AES-GCM 加密，只保存在手表本机；本 App 不申请网络权限。请只在 PayPal 官方页面使用填充键盘。");
        note.setTextSize(11);
        note.setTextColor(0xFF9E9E9E);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap(0));

        setContentView(scroll);
        refreshStatus();
    }

    private void refreshStatus() {
        if (savedStatus == null) return;
        if (CredentialStore.hasCredential(this)) {
            String account = CredentialStore.getAccount(this);
            savedStatus.setText("已保存登录：" + maskAccount(account));
            if (accountInput != null && accountInput.getText().length() == 0) {
                accountInput.setText(account);
            }
        } else {
            savedStatus.setText("尚未保存 PayPal 登录");
        }
    }

    private void saveCredential() {
        String account = accountInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (account.isEmpty()) {
            toast("请输入 PayPal 账号");
            return;
        }
        if (password.isEmpty()) {
            toast("请输入 PayPal 密码");
            return;
        }
        try {
            CredentialStore.saveCredential(this, account, password);
            passwordInput.setText("");
            refreshStatus();
            toast("已加密保存在手表本机");
        } catch (Exception e) {
            toast("保存失败");
        }
    }

    private void deleteCredential() {
        CredentialStore.deleteCredential(this);
        accountInput.setText("");
        passwordInput.setText("");
        refreshStatus();
        toast("已删除本机保存的登录");
    }

    private void openKeyboardSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
        } catch (Exception e) {
            toast("请在手表设置里启用 PayPal Fill Keyboard");
        }
    }

    private void showKeyboardPicker() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            } else {
                toast("请在输入法设置中选择 PayPal Fill Keyboard");
            }
        } catch (Exception e) {
            toast("请在输入法设置中选择 PayPal Fill Keyboard");
        }
    }

    private void openPayPalOnWatch() {
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

        CredentialStore.markExpectedPayPalLaunch(this);

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(SAMSUNG_INTERNET_PACKAGE);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("请先在手表上安装 Samsung Browser");
        } catch (Exception e) {
            toast("无法在手表浏览器打开 PayPal");
        }
    }

    private String maskAccount(String account) {
        if (account == null || account.isEmpty()) return "已保存";
        int at = account.indexOf('@');
        if (at > 1) {
            return account.substring(0, 1) + "***" + account.substring(at);
        }
        if (account.length() <= 3) return "***";
        return account.substring(0, 2) + "***" + account.substring(account.length() - 1);
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
