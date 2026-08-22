package com.openai.paypalreceiver;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {
    @Test
    public void launchesBoaWithdrawalScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("PayPal 收款 → BOA")).check(matches(isDisplayed()));
            onView(withText("允许手表通知")).check(matches(isDisplayed()));
            onView(withText("打开 PayPal 提现到 BOA")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void usesOfficialPayPalWithdrawalUrl() {
        assertEquals("https://www.paypal.com/myaccount/money/balances/withdraw", MainActivity.PAYPAL_WITHDRAW_URL);
    }

    @Test
    public void missingSamsungBrowserDoesNotCrash() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("打开 PayPal 提现到 BOA")).perform(click());
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }
}
