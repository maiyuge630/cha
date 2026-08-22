package com.openai.paypalreceiver;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {
    @Test
    public void launchesReceiverScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("PayPal 收款提醒")).check(matches(isDisplayed()));
            onView(withText("允许手表通知")).check(matches(isDisplayed()));
            onView(withText("打开 PayPal 交易记录")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void missingSamsungBrowserDoesNotCrash() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("打开 PayPal 交易记录")).perform(click());
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }
}
