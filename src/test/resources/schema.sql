-- updated_at を自動更新する関数
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS '
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
' LANGUAGE plpgsql;

-- ENUM: 出版状況
CREATE TYPE publication_status AS ENUM ('unpublished', 'published');

-- 著者テーブル
CREATE TABLE authors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    birth_date      DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_birth_date CHECK (birth_date < current_date)
);

-- 著者テーブルのupdated_atを更新するトリガー
CREATE TRIGGER set_timestamp_authors
BEFORE UPDATE ON authors
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

-- 書籍テーブル
CREATE TABLE books (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255) NOT NULL,
    price           NUMERIC(10,2) NOT NULL,
    status          publication_status NOT NULL DEFAULT 'unpublished',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_price_nonnegative CHECK (price >= 0)
);

-- 書籍テーブルのupdated_atを更新するトリガー
CREATE TRIGGER set_timestamp_books
BEFORE UPDATE ON books
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

-- publication_statusの変更を制限する関数
CREATE OR REPLACE FUNCTION check_publication_status_change()
RETURNS TRIGGER AS '
BEGIN
    IF OLD IS NOT NULL THEN
        IF OLD.status = ''published'' AND NEW.status = ''unpublished'' THEN
            RAISE EXCEPTION ''Publication status cannot be changed from published to unpublished.'';
        END IF;
    END IF;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

-- 書籍テーブルのstatus変更を監視するトリガー
CREATE TRIGGER check_status_change_before_update
BEFORE UPDATE ON books
FOR EACH ROW
EXECUTE FUNCTION check_publication_status_change();

-- 中間テーブル（多対多）
CREATE TABLE book_authors (
    book_id         UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES authors(id) ON DELETE RESTRICT,
    PRIMARY KEY (book_id, author_id)
);

-- 中間テーブルの外部キーにインデックスを作成
CREATE INDEX idx_book_authors_book_id ON book_authors(book_id);
CREATE INDEX idx_book_authors_author_id ON book_authors(author_id);
