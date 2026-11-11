# Đánh giá Hợp đồng OpenAPI (`docs/review_openapi.md`)

## 1. Bảng kiểm tra các thao tác API

| Thao tác | Mã thành công | Mã lỗi đã khai báo | An toàn? | Idempotent? |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/books` | `200` | `400` | có | có |
| `POST /api/v1/books` | `201` + `Location` • `ETag` | `400`, `409` | không | không |
| `GET /api/v1/books/{isbn}` | `200` + `ETag` / `304` | `404` | có | có |
| `PUT /api/v1/books/{isbn}` | `200` + `ETag` mới | `400`, `404`, `412`, `428` | không | có |
| `DELETE /api/v1/books/{isbn}` | `204` | `404` | không | có |

---

## 2. Kiểm tra mã trạng thái HTTP (RFC 9110 / RFC 6585)

Tất cả các mã trạng thái được khai báo trong hợp đồng OpenAPI đều chính xác ngữ nghĩa theo chuẩn HTTP RFC 9110 và RFC 6585:

- **`200 OK`**: Trả về khi truy vấn thành công cho `GET` (danh sách hoặc chi tiết sách) và `PUT` (kèm `ETag` mới).
- **`201 Created`**: Trả về khi tạo mới sách thành công qua `POST`, đi kèm tiêu đề `Location` chứa URI của tài nguyên và `ETag`.
- **`204 No Content`**: Trả về khi xóa thành công qua `DELETE` và không kèm nội dung response body.
- **`304 Not Modified`**: Trả về khi `GET /api/v1/books/{isbn}` có tiêu đề `If-None-Match` trùng khớp với `ETag` hiện tại của tài nguyên (dữ liệu chưa bị thay đổi, giúp tiết kiệm băng thông).
- **`400 Bad Request`**: Dữ liệu đầu vào hoặc thông số truy vấn không hợp lệ (vi phạmValidation).
- **`404 Not Found`**: Không tìm thấy sách tương ứng với mã ISBN trong hệ thống.
- **`409 Conflict`**: Xảy ra khi tạo mới cuốn sách nhưng mã ISBN đã tồn tại trên hệ thống.
- **`412 Precondition Failed`**: Giá trị `ETag` trong header `If-Match` gửi lên không trùng với `ETag` hiện tại trên server (dữ liệu đã bị thay đổi bởi người khác trước đó).
- **`428 Precondition Required`**: Yêu cầu bắt buộc phải đính kèm header `If-Match` khi thực hiện `PUT` (theo RFC 6585).

---

## 3. Lý do thao tác `PUT` cần header `If-Match`

Thao tác `PUT` sử dụng cơ chế **Optimistic Concurrency Control (Kiểm soát truy cập đồng thời lạc quan)** thông qua tiêu đề `If-Match` nhằm:
- **Tránh sự cố Lost Update (Cập nhật bị mất)**: Khi hai người dùng cùng đọc tài nguyên ở phiên bản cũ và cùng gửi yêu cầu sửa đổi. Nếu không kiểm tra phiên bản, yêu cầu cập nhật đến sau sẽ vô tình ghi đè và làm mất các thay đổi của người cập nhật trước.
- **Cơ chế xử lý**:
  1. Client đọc thông tin sách và nhận về `ETag`.
  2. Khi cập nhật qua `PUT`, client phải đính kèm header `If-Match` chứa `ETag` vừa nhận.
  3. Nếu thiếu `If-Match` $\rightarrow$ Server từ chối bằng lỗi `428 Precondition Required`.
  4. Nếu `ETag` trên Server đã thay đổi $\rightarrow$ Server từ chối bằng lỗi `412 Precondition Failed`.

---

## 4. Kiểm tra khớp ràng buộc với XSD ở Bài thực hành 1

- **`Isbn13`**: 
  - Pattern OpenAPI: `^97[89][0-9]{10}$`
  - Khớp hoàn toàn với định dạng chuẩn ISBN-13 định nghĩa ở Bài thực hành 1 (chuỗi 13 chữ số bắt đầu bằng 978 hoặc 979).
- **`Money`**:
  - OpenAPI: `minimum: 0`, `multipleOf: 0.01`, `currency` thuộc enum `[VND, USD]`.
  - Khớp hoàn toàn với schema XSD ở Bài thực hành 1, đảm bảo số tiền không âm và chia hết cho $0.01$ (độ chính xác đơn vị tiền tệ).

---

## 5. Đánh giá tính tương thích ngược (Backward Compatibility)

- **Câu hỏi**: Nếu thêm một trường bắt buộc mới (required field) vào `BookCreateRequest`, có phá vỡ tương thích ngược không?
- **Trả lời**: **CÓ (Gây breaking change)**.
- **Lý do**:
  - Các client ứng dụng cũ chưa được cập nhật sẽ gửi các request POST không bao gồm trường mới này.
  - Khi server yêu cầu trường mới là bắt buộc (`required`), toàn bộ request từ client cũ sẽ bị vi phạm Validation và bị server từ chối với lỗi `400 Bad Request`.
- **Giải pháp khuyến nghị**:
  - Đưa trường mới thành tùy chọn (optional) và đặt giá trị mặc định (`default`) phía server.
  - Hoặc nâng phiên bản API (API Versioning, ví dụ chuyển route sang `/api/v2/books`).
