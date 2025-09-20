# 📕 書籍管理API
書籍と著者の情報を管理するためのWeb API  

### 🛠 技術スタック  
・Language: Kotlin  
・Framework: Spring Boot  
・Database: PostgreSQL  
・DB Migration: Flyway  
・DB Access: jOOQ  
・Build Tool: Gradle  
・Container: Docker, Docker Compose  
・Testing: Junit, Mockito, Testcontainers

### 🏃‍➡️ 起動手順
前提条件  
・DockerとDocker Composeがインストールされていること  
・JDK 17

1.リポジトリをクローン
```bash
git clone https://github.com/takumi616/bookmanager.git
```
2.環境変数、プロパティファイルの準備
```bash
cp .env.sample .env
cp gradle.properties.sample gradle.properties
```

3.Postgresコンテナを起動
```bash
docker compose up -d db
```
4.PostgresコンテナにDBマイグレーションを適用
```bash
./gradlew flywayMigrate
```
5.JOOQコード生成
```bash
./gradlew jooqCodegen
```
6.Kotlinイメージのビルド
```bash
docker compose build app
```
7.Kotlinコンテナ起動
```bash
docker compose up app
```

### ⌘ コマンドサンプル
#### ・著者情報登録
リクエストCurlコマンド
```bash
curl -X POST http://localhost:8080/authors \
-H "Content-Type: application/json" \
-d '{"name": "サンプル著者", "birthDate": "2000-12-25"}'
```
レスポンス例
```bash
{
  "id":"3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
  "name":"サンプル著者",
  "birthDate":"2000-12-25"
}
```

#### ・著者情報更新
リクエストCurlコマンド
```bash
curl -X PUT http://localhost:8080/authors/3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294 \
-H "Content-Type: application/json" \
-d '{"name": "サンプル著者（改）", "birthDate": "2000-12-24"}'
```
レスポンス例
```bash
{
  "id":"3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
  "name":"サンプル著者（改）",
  "birthDate":"2000-12-24"
}
```

#### ・書籍情報登録
リクエストCurlコマンド
```bash
curl -X POST http://localhost:8080/books \
-H "Content-Type: application/json" \
-d '{"title": "Kotlin spring boot", "price": 2000, "authorIdList": ["3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294"], "status": "unpublished"}'
```
レスポンス例
```bash
{
  "id":"df89f7c2-a008-4a78-a2f1-ec35ea0ad2d3",
  "title":"Kotlin spring boot",
  "price":2000.00,
  "status":"unpublished",
  "authorList":[
  {
    "id":"3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
    "name":"サンプル著者（改）",
    "birthDate":"2000-12-24"
  }
  ]
}
```

#### ・書籍情報更新（※事前に著者をもう一人追加しています）
リクエストCurlコマンド
```bash
curl -X PUT http://localhost:8080/books/df89f7c2-a008-4a78-a2f1-ec35ea0ad2d3 \
-H "Content-Type: application/json" \
-d '{
  "title": "Kotlin spring boot 改訂版",
  "price": 2500,
  "authorIdList": [
    "3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
    "5762a0bb-9ce3-4547-abb5-f434dc534ea3"
  ],
  "status": "published"
}'
```
レスポンス例
```bash
{
  "id":"df89f7c2-a008-4a78-a2f1-ec35ea0ad2d3",
  "title":"Kotlin spring boot 改訂版",
  "price":2500.00,
  "status":"published",
  "authorList":[
  {
    "id":"3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
    "name":"サンプル著者（改）",
    "birthDate":"2000-12-24"
  },
  {
    "id":"5762a0bb-9ce3-4547-abb5-f434dc534ea3",
    "name":"テスト著者",
    "birthDate":"1995-12-25"
  }
  ]
}
```

#### ・著者に紐づく書籍一覧取得（※リスト取得を確認するため事前に書籍を一つ追加しています）
リクエストCurlコマンド
```bash
curl -X GET http://localhost:8080/authors/3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294/books
```
レスポンス例
```bash
{
  "author":
  {
    "id":"3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
    "name":"サンプル著者（改）",
    "birthDate":"2000-12-24"
  },
  "bookList":[
  {
    "id":"df89f7c2-a008-4a78-a2f1-ec35ea0ad2d3",
    "title":"Kotlin spring boot 改訂版",
    "price":2500.00,
    "status":"published",
    "authorList":[
    {
      "id":"3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
      "name":"サンプル著者（改）",
      "birthDate":"2000-12-24"
    },
    {
      "id":"5762a0bb-9ce3-4547-abb5-f434dc534ea3",
      "name":"テスト著者",
      "birthDate":"1995-12-25"
    }
    ]
  },
  {
    "id":"5784fdec-e229-4da7-b377-ddc0dc2697cf",
    "title":"Backend Kotlin",
    "price":3000.00,
    "status":"unpublished",
    "authorList":[
    {
      "id":"3eea38b0-d6f0-4bb8-b33f-c4c37cfe4294",
      "name":"サンプル著者（改）",
      "birthDate":"2000-12-24"
    }
    ]
  }
  ]
}
```

### 単体テストの実行とカバレッジ出力
```bash
./gradlew test
```
カバレッジ出力先  
build/reports/jacoco/test/html/index.html