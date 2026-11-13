# Sân Nhà — Quản lý thuê sân bóng

Ứng dụng Android bằng **Java + XML + SQLite**, package `com.example.baigiuaki`, chạy từ **Android 7.0 / API 24**. Giao diện gọn, dùng đỏ lấy cảm hứng từ Manchester United và phối xanh sân cỏ ở màn đặt sân. Nhãn, thông báo và lỗi đều bằng tiếng Việt có dấu.

![Thiết kế năm màn hình](docs/Thiet_ke_5_man_hinh.png)

Ảnh trên là bản thiết kế, không phải ảnh chụp emulator. XML triển khai cùng bảng màu, thứ tự thông tin và các thao tác. Bộ chọn ngày/giờ dùng thành phần Android; nội dung cuộn trên màn hình nhỏ. Dữ liệu và ngày trong ảnh chỉ để minh họa.

## Mở và chạy

1. Clone hoặc tải ZIP toàn bộ repo, mở thư mục gốc bằng Android Studio.
2. Chọn **Gradle JDK 25**, theo cấu hình Gradle đã có trong dự án. Cho phép Android Studio tải SDK khi được yêu cầu.
3. Cài **Android SDK Platform 37** và **Build Tools 36.0.0** qua SDK Manager. Repo giữ AGP **9.4.1**, Gradle **9.6.0**, `compileSdk/targetSdk = 37`, `minSdk = 24`.
4. Chờ Gradle Sync, chọn emulator hoặc thiết bị Android API 24 trở lên rồi nhấn **Run app**.

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

macOS/Linux:

```bash
bash ./gradlew assembleDebug
bash ./gradlew testDebugUnitTest lintDebug
bash ./gradlew connectedDebugAndroidTest
```

APK nằm ở `app/build/outputs/apk/debug/app-debug.apk`. Lệnh `connectedDebugAndroidTest` cần một emulator đang chạy hoặc điện thoại bật USB debugging. Không cần máy chủ, tài khoản, Internet lúc sử dụng ứng dụng, hay quyền truy cập bộ nhớ chung.

## Chức năng

| Yêu cầu | Cách triển khai |
|---|---|
| Danh sách sân | Mã sân, tên, địa chỉ, giá/giờ, trạng thái theo ngày–giờ–thời lượng đang chọn |
| Thêm sân | Kiểm tra đủ thông tin, mã duy nhất không phân biệt hoa/thường, đơn giá dương |
| Xóa sân | Có hộp xác nhận; chặn khi còn đơn đang diễn ra/sắp tới; giữ lịch sử đơn cũ |
| Đặt sân | Chọn sân, họ tên, điện thoại, ngày, giờ bắt đầu và số giờ thuê |
| Tính tiền | Đơn giá × số giờ, dùng `long` để tránh tràn số tiền |
| Chặn trùng | Kiểm tra và chèn đơn trong cùng giao dịch SQLite |
| Quản lý đơn | Danh sách, lọc tất cả/đã đặt/đã hủy, xem chi tiết, hủy có xác nhận |
| Hủy đơn | Lưu trạng thái đã hủy và giải phóng đúng khung giờ; không xóa lịch sử |
| Lưu bền vững | SQLite trong vùng dữ liệu riêng; đóng/mở app vẫn giữ thông tin |
| Điều hướng | Intent giữa năm Activity; truyền ID sân, thời điểm, thời lượng và ID đơn |

Lần chạy đầu có **3 sân minh họa và chưa có đơn đặt**. Dữ liệu mẫu chỉ được tạo khi tạo cơ sở dữ liệu lần đầu; xóa hết sân rồi mở lại không tự sinh lại. Xóa dữ liệu ứng dụng hoặc gỡ cài đặt sẽ xóa cơ sở dữ liệu cục bộ.

## Quy tắc nghiệp vụ

- Trạng thái sân được tính theo khung thời gian chọn, không phải một cờ bận cho cả ngày.
- Hai đơn trùng khi: `bắt đầu cũ < kết thúc mới` và `kết thúc cũ > bắt đầu mới`.
- Ví dụ: sân đã đặt 18:00–20:00 thì 19:00–21:00 bị chặn; **20:00–21:00 được phép**.
- Đơn qua nửa đêm dùng thời điểm tuyệt đối nên vẫn được kiểm tra đúng ở ngày tiếp theo.
- Đơn đã hủy không tham gia kiểm tra trùng. Nếu còn đơn khác trùng khung giờ, sân vẫn báo bận.
- Đơn lưu bản chụp mã/tên/địa chỉ/đơn giá của sân lúc đặt. Xóa sân không làm mất thông tin trên đơn.
- Xóa sân là ẩn khỏi danh sách; mã sân cũ vẫn được giữ để tránh nhầm lịch sử. Sân còn lịch đang diễn ra hoặc sắp tới phải hủy các đơn đó trước.
- Số giờ thuê là số nguyên **1–24**; giá là số nguyên **1–999.999.999 đồng/giờ**; điện thoại **9–15 chữ số**, cho phép `+` ở đầu và khoảng trắng/dấu phân cách.
- Không cho đặt vào quá khứ. Giờ hiển thị và chọn theo múi giờ Việt Nam `Asia/Ho_Chi_Minh`.
- Nút lưu bị khóa khi đang xử lý; thao tác SQLite chạy trên luồng nền. Các truy vấn danh sách bỏ qua kết quả cũ khi người dùng đổi bộ lọc nhanh.
- Màn hình lưu lựa chọn và nội dung nhập khi Activity được tạo lại; nội dung dài có thể cuộn, không khóa hướng màn hình.

## Mã giao diện tương ứng ảnh

Android Views dùng **XML**, không dùng YML để dựng giao diện. File YML trong `.github/workflows` chỉ cấu hình kiểm thử trên GitHub Actions.

| Màn hình | XML | Java |
|---|---|---|
| Danh sách sân | `activity_main.xml`, `header_pitch_list.xml`, `item_pitch.xml` | `MainActivity.java` |
| Thêm sân | `activity_add_pitch.xml` | `AddPitchActivity.java` |
| Đặt sân | `activity_booking.xml` | `BookingActivity.java` |
| Danh sách đơn | `activity_bookings.xml`, `item_booking.xml` | `BookingsActivity.java` |
| Chi tiết đơn | `activity_booking_detail.xml` | `BookingDetailActivity.java` |

XML nằm trong `app/src/main/res/layout`; Java nằm trong `app/src/main/java/com/example/baigiuaki`. Màu và kiểu dùng chung ở `res/values/colors.xml`, `styles.xml`, `themes.xml`; hình nền/nút/icon là XML trong `res/drawable`.

| Thành phần | Màu |
|---|---|
| Nút chính, nhấn mạnh | `#DA291C` |
| Thẻ chọn sân trong màn đặt | `#15803D` |
| Nền xanh nhạt | `#E8F5E9` |
| Nền chung | `#F7F7F8` |
| Chữ chính | `#171717` |

## Kiểm thử và nộp bài

- `BookingRulesTest`: nhập liệu, tính tiền lớn, chuẩn hóa dữ liệu và biên hai khoảng thời gian.
- `DatabaseTest`: SQLite thực trên Android; lưu/mở lại, trùng lịch, hai sân cùng giờ, hủy, xóa, qua nửa đêm và hai yêu cầu đồng thời.
- `ScreenTest`: mở các màn hình, thêm sân, nhập thiếu, tạo lại Activity, đặt/xem/hủy đơn; chụp ảnh màn hình emulator.
- Workflow **Kiểm thử Android** build APK, chạy unit test/lint và kiểm thử trên emulator **API 24, API 35**. Kết quả thực tế xem tại [Actions](https://github.com/Khangvu345/GiuaKi-App-155/actions); chỉ run màu xanh mới xác nhận các bước đã đạt.
- Artifact `SanNha-APK` chứa APK; artifact `Kiem-thu-API-*` chứa báo cáo và ảnh chụp màn hình nếu các bước tương ứng hoàn tất.
- Khi nộp bài, nộp toàn bộ repo/ZIP cùng APK đã build. Xem [kịch bản nghiệm thu](docs/KIEM_THU.md).

## Tài liệu tham khảo

- [Android XML layouts](https://developer.android.com/develop/ui/views/layout/declaring-layout)
- [SQLite trên Android](https://developer.android.com/training/data-storage/sqlite)
- [Tương thích Android Gradle Plugin 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes)

Commit theo Conventional Commits, ví dụ `feat: xây dựng ứng dụng quản lý thuê sân bóng`, `fix: sửa kiểm tra trùng giờ`.
