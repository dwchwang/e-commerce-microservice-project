# .latexmkrc – Cấu hình latexmk cho dự án ĐATN
# File này bổ sung cho settings.json, dùng khi chạy latexmk từ terminal

# Sử dụng pdflatex
$pdf_mode = 1;
$pdflatex = 'pdflatex -synctex=1 -interaction=nonstopmode -file-line-error %O %S';

# Thư mục đầu ra – latexmk sẽ gom toàn bộ file phụ trợ và PDF vào đây
$out_dir = 'build';

# Bibliography backend do biblatex/latexmk tu phat hien (biber trong DoAn.tex)
$bibtex_use = 2;  # chay bibtex/biber khi can, xoa .bbl khi clean

# Tự động chạy đủ số vòng để cross-reference ổn định
$max_repeat = 5;

# Mở PDF sau khi build (comment ra nếu không cần)
# $pdf_previewer = 'open -a Preview';

# Dọn sạch thêm các loại file phụ trợ khi chạy: latexmk -C
push @generated_exts, 'synctex.gz', 'synctex.gz(busy)', 'run.xml', 'bcf',
                       'nav', 'snm', 'vrb', 'bbl', 'fls', 'fdb_latexmk';
