package com.example.baigiuaki;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseActivity extends Activity {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final AtomicInteger PENDING = new AtomicInteger();
    private static volatile Runnable idleListener;
    protected AppDatabase db;
    interface Result<T> { void accept(T value); }

    @Override protected void attachBaseContext(Context base) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(new Locale("vi", "VN"));
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = AppDatabase.get(this);
    }

    protected void screen(int layout) {
        setContentView(layout);
        View root = findViewById(R.id.root);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
                return insets;
            });
            root.requestApplyInsets();
        }
        if (Build.VERSION.SDK_INT >= 26) getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    protected <T> void task(Callable<T> work, Result<T> success) { task(work, success, error -> showError(error)); }

    protected <T> void task(Callable<T> work, Result<T> success, Result<Exception> failure) {
        PENDING.incrementAndGet();
        IO.execute(() -> {
            try {
                T result = work.call();
                complete(() -> success.accept(result));
            } catch (Exception error) {
                complete(() -> failure.accept(error));
            }
        });
    }

    private void complete(Runnable action) {
        runOnUiThread(() -> {
            try { if (!isFinishing() && !isDestroyed()) action.run(); }
            finally {
                if (PENDING.decrementAndGet() == 0 && idleListener != null) idleListener.run();
            }
        });
    }

    // Cho kiểm thử giao diện chờ tác vụ SQLite hoàn tất.
    static boolean isIdle() { return PENDING.get() == 0; }
    static void setIdleListener(Runnable listener) { idleListener = listener; }

    protected void showError(Exception error) {
        String message = error instanceof IllegalArgumentException ? error.getMessage()
                : "Không thể xử lý dữ liệu lúc này. Vui lòng thử lại.";
        new AlertDialog.Builder(this).setTitle("Thông báo").setMessage(message).setPositiveButton("Đóng", null).show();
    }

    protected void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    protected void confirmCancel(Models.Booking booking, Runnable after) {
        new AlertDialog.Builder(this).setTitle("Hủy đơn đặt sân?")
                .setMessage("Bạn muốn hủy " + Ui.code(booking.id) + " của " + booking.customer + "? Khung giờ này sẽ được giải phóng.")
                .setNegativeButton("Giữ đơn", null).setPositiveButton("Hủy đơn", (d, w) -> task(() -> {
                    db.cancel(booking.id); return true;
                }, done -> { toast("Đã hủy đơn đặt sân."); after.run(); })).show();
    }
}
