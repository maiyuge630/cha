package com.openai.paypalwearshortcut;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {
    private Context context;

    @Before
    public void cleanState() {
        context = ApplicationProvider.getApplicationContext();
        PasswordVault.delete(context);
        SecureClipboard.clearIfOwned(context);
    }

    @After
    public void cleanUp() {
        PasswordVault.delete(context);
        SecureClipboard.clearIfOwned(context);
    }

    @Test
    public void launchesWithLockedRecipientAndUsd() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("PayPal 转账")).check(matches(isDisplayed()));
            onView(withText("固定收款人：2137765821\n固定币种：USD 美元"))
                    .check(matches(isDisplayed()));
            onView(withText("登录密码：尚未保存")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void savesPasswordWithoutShowingItAgain() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withHint("PayPal 密码（只需保存一次）"))
                    .perform(replaceText("test-password-123"));
            onView(withText("保存 / 更新密码")).perform(click());
            onView(withText("登录密码：已安全保存")).check(matches(isDisplayed()));
            assertFalse("Password input should be cleared after save",
                    PasswordVault.get(context).isEmpty());
        }
    }

    @Test
    public void invalidAmountDoesNotCrashOrFinishActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withHint("金额，例如 25.00")).perform(scrollTo(), replaceText("0"));
            onView(withText("付款")).perform(scrollTo(), click());
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }

    @Test
    public void missingSamsungBrowserDoesNotCopyPasswordOrCrash() throws Exception {
        PasswordVault.save(context, "test-password-123");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withHint("金额，例如 25.00")).perform(scrollTo(), replaceText("1.25"));
            onView(withText("付款")).perform(scrollTo(), click());
            assertFalse("Password must not be copied when browser is unavailable",
                    SecureClipboard.isOwnedClip(context));
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }

    @Test
    public void repeatedPaymentTapsDoNotCrash() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withHint("金额，例如 25.00")).perform(scrollTo(), replaceText("2.00"));
            for (int i = 0; i < 3; i++) {
                onView(withText("付款")).perform(scrollTo(), click());
            }
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }
}
