package com.openai.paypalreceiver;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {
    @Test
    public void launchesSetupScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("PayPal 收款 → BOA · 手机端")).check(matches(isDisplayed()));
            onView(withText("开启通知访问")).check(matches(isDisplayed()));
            onView(withText("发送测试收款提醒到手表")).check(matches(isDisplayed()));
        }
    }
}
