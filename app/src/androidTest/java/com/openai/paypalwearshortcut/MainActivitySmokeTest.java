package com.openai.paypalwearshortcut;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {
    @Test
    public void launchesWithLockedRecipientAndUsd() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("PayPal 转账")).check(matches(isDisplayed()));
            onView(withText("固定收款人：2137765821")).check(matches(isDisplayed()));
            onView(withText("固定币种：USD 美元")).check(matches(isDisplayed()));
            onView(withText("仅在手表上付款")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invalidAmountDoesNotCrashOrFinishActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withHint("金额，例如 25.00")).perform(replaceText("0"));
            onView(withText("仅在手表上付款")).perform(click());
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }

    @Test
    public void missingSamsungBrowserIsHandledWithoutCrash() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withHint("金额，例如 25.00")).perform(replaceText("1.25"));
            onView(withText("仅在手表上付款")).perform(click());
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }

    @Test
    public void repeatedPaymentTapsDoNotCrash() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withHint("金额，例如 25.00")).perform(replaceText("2.00"));
            for (int i = 0; i < 3; i++) {
                onView(withText("仅在手表上付款")).perform(click());
            }
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }
}
