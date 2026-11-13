package com.example.baigiuaki;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ScreenTest {
    private Context context;
    private AppDatabase db;
    private final IdlingResource idle = new IdlingResource() {
        @Override public String getName() { return "Tác vụ SQLite"; }
        @Override public boolean isIdleNow() { return BaseActivity.isIdle(); }
        @Override public void registerIdleTransitionCallback(ResourceCallback callback) {
            BaseActivity.setIdleListener(callback::onTransitionToIdle);
        }
    };

    @Before public void chuanBi() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext(); db = AppDatabase.get(context);
        IdlingRegistry.getInstance().register(idle);
    }
    @After public void donDep() {
        IdlingRegistry.getInstance().unregister(idle); BaseActivity.setIdleListener(null);
    }

    private void chupAnh(String name) throws Exception {
        onIdle();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File dir = new File(context.getExternalFilesDir(null), "screenshots");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        try (FileOutputStream stream = new FileOutputStream(new File(dir, name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally { image.recycle(); }
    }

    @Test public void themSanKiemTraNhapThieuVaGiuDuLieuKhiXoay() throws Exception {
        String code = "UI" + System.currentTimeMillis();
        try (ActivityScenario<AddPitchActivity> scenario = ActivityScenario.launch(AddPitchActivity.class)) {
            onView(withId(R.id.btnSavePitch)).perform(click());
            onView(withId(R.id.etCode)).check(matches(hasErrorText("Vui lòng nhập mã sân.")));
            onView(withId(R.id.etCode)).perform(replaceText(code), closeSoftKeyboard());
            onView(withId(R.id.etPitchName)).perform(replaceText("Sân thử giao diện"), closeSoftKeyboard());
            onView(withId(R.id.etAddress)).perform(replaceText("46 Nguyễn Trãi, Hà Nội"), closeSoftKeyboard());
            onView(withId(R.id.etPrice)).perform(replaceText("300000"), closeSoftKeyboard());
            scenario.recreate();
            onView(withId(R.id.etPitchName)).check(matches(withText("Sân thử giao diện")));
            chupAnh("02-them-san");
            onView(withId(R.id.btnSavePitch)).perform(click()); onIdle();
            boolean found = false;
            for (Models.Pitch p : db.pitches(Ui.nextHour(), 2)) if (p.code.equals(code)) found = true;
            assertTrue(found);
        }
    }

    @Test public void datSanXemChiTietVaHuyDon() throws Exception {
        long pitchId = db.addPitch("BK" + System.currentTimeMillis(), "Sân Mini kiểm thử", "12 Nguyễn Trãi, Hà Nội", 300000);
        long start = Ui.nextHour() + 24 * BookingRules.HOUR_MS;
        Intent intent = new Intent(context, BookingActivity.class).putExtra("pitch_id", pitchId).putExtra("start", start).putExtra("hours", 2);
        try (ActivityScenario<BookingActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnConfirm)).perform(click());
            onView(withId(R.id.etCustomer)).check(matches(hasErrorText("Vui lòng nhập họ tên.")));
            onView(withId(R.id.etCustomer)).perform(replaceText("Nguyễn Minh Anh"), closeSoftKeyboard());
            onView(withId(R.id.etPhone)).perform(replaceText("0912345678"), closeSoftKeyboard());
            onView(withId(R.id.tvTotal)).check(matches(withText("600.000 đ")));
            scenario.recreate();
            onView(withId(R.id.etCustomer)).check(matches(withText("Nguyễn Minh Anh")));
            chupAnh("03-dat-san");
            onView(withId(R.id.btnConfirm)).perform(click());
            onView(withId(R.id.tvDetailTotal)).check(matches(withText("600.000 đ")));
            chupAnh("05-chi-tiet-don");
            assertFalse(db.available(pitchId, start, 2));
            onView(withId(R.id.btnCancelDetail)).perform(click());
            onView(withText("Hủy đơn")).perform(click());
            onView(withId(R.id.tvDetailStatus)).check(matches(withText("Đã hủy")));
            assertTrue(db.available(pitchId, start, 2));
            pressBackUnconditionally();
        }
    }

    @Test public void moDanhSachSanVaDanhSachDon() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnAdd)).check(matches(isDisplayed()));
            onView(withId(R.id.filterHours)).check(matches(withText("2 giờ")));
            chupAnh("01-danh-sach-san");
            scenario.recreate();
            onView(withId(R.id.filterHours)).check(matches(withText("2 giờ")));
            onView(withId(R.id.navBookings)).perform(click());
            onView(withId(R.id.tabAll)).check(matches(isDisplayed()));
            onView(withId(R.id.tabCancelled)).perform(click());
            onView(withId(R.id.tabAll)).perform(click());
            chupAnh("04-don-dat-san");
            onView(withId(R.id.navPitches)).perform(click());
            onView(withId(R.id.btnAdd)).check(matches(isDisplayed()));
        }
    }
}
