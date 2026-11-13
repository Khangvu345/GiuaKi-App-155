# Kịch bản nghiệm thu

Chạy trên dữ liệu thử nghiệm. Chọn ngày trong tương lai để tránh quy tắc chặn đặt quá khứ.

| Mã | Thao tác | Kết quả cần đạt |
|---|---|---|
| TC01 | Mở ứng dụng lần đầu | Hiện 3 sân mẫu, đủ mã/tên/địa chỉ/giá/trạng thái |
| TC02 | Thêm sân đầy đủ | Sân xuất hiện ở danh sách và hộp chọn sân |
| TC03 | Bỏ trống từng trường | Có thông báo tiếng Việt; không lưu dữ liệu thiếu |
| TC04 | Giá bằng 0, mã trùng hoa/thường | Từ chối lưu; dữ liệu trước đó còn nguyên |
| TC05 | Thuê 2 giờ với giá 300.000 đ | Tổng tiền 600.000 đ trước và sau lưu |
| TC06 | Tạo đơn 18:00–20:00; đặt lại cùng sân 19:00–21:00 | Từ chối đơn trùng |
| TC07 | Lịch cũ 18:00–20:00; thử 17:00–19:00, 18:00–20:00, 17:00–21:00 | Đều từ chối |
| TC08 | Lịch cũ 18:00–20:00; đặt 20:00–21:00 | Cho phép |
| TC09 | Hai sân khác nhau, cùng giờ | Cho phép cả hai đơn |
| TC10 | Đặt 23:00–01:00; thử đặt cùng sân 00:00–01:00 hôm sau | Từ chối; chi tiết thể hiện ngày kết thúc |
| TC11 | Hủy đơn rồi đặt lại đúng giờ | Đơn cũ hiện Đã hủy; đơn mới lưu được nếu không còn lịch khác |
| TC12 | Xóa sân còn đơn sắp tới | Bị chặn kèm hướng dẫn hủy đơn trước |
| TC13 | Hủy các đơn tương lai rồi xóa sân | Sân biến mất khỏi danh sách; lịch sử đơn còn đầy đủ |
| TC14 | Tắt hoàn toàn rồi mở lại ứng dụng | Sân và đơn giữ nguyên; trạng thái tính đúng |
| TC15 | Xoay màn hình ở biểu mẫu đang nhập | Họ tên, điện thoại, sân, ngày/giờ và thời lượng được giữ |
| TC16 | Bấm lưu liên tục | Không tạo nhiều đơn; nút khóa khi đang lưu |
| TC17 | Điện thoại sai, tên chỉ khoảng trắng, giờ quá khứ | Có lỗi tiếng Việt, không tạo đơn |
| TC18 | Lọc Tất cả / Đã đặt / Đã hủy | Danh sách và số lượng khớp trạng thái |
| TC19 | Hủy một đơn trong khi sân vẫn có lịch khác | Chỉ giải phóng khung giờ của đơn bị hủy |
| TC20 | Mở trên thiết bị nhỏ, xoay ngang, mở bàn phím | Có thể cuộn nội dung và thao tác nút lưu |

Ảnh `Thiet_ke_5_man_hinh.png` là bản thiết kế. Ảnh chụp khi chạy thật nằm trong artifact kiểm thử của GitHub Actions sau khi job emulator thành công.

Kiểm thử tự động không thay thế hoàn toàn thao tác kiểm tra bằng tay trên thiết bị nộp bài. Đọc trạng thái run Actions gắn với đúng commit trước khi kết luận đã đạt.
