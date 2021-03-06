# NetworkWebCamera

Androidスマートフォンのカメラ映像をSocketを通じてリアルタイム送信し，Webカメラのように使える？やつ  
受信にはPCで https://github.com/Nanamiiiii/NetworkWebCameraHost を使用

## 動作要件
- Android 5.0 以上　(SDK 21)
- ミドルレンジ以上のCPU (比較的高負荷なので多コア推奨)

## 使用方法
PC側アプリケーションを起動しで待機状態にした後，PCのローカルIPアドレスとポートを指定して接続  
接続完了すると自動で送信が始まります．終了はEXITボタンかバックキーで．  
ローカルネットワークでの使用が前提のため，Wi-Fi接続が検出されない場合は終了するようになっています．

## スクリーンショット
<center>
<img src = "docs/screenshots/ss_1.png" width = "40%">
<img src = "docs/screenshots/ss_3.png" width = "40%">

<img src = "docs/screenshots/ss_2.png" width = "40%">
<img src = "docs/screenshots/ss_4.png" width = "40%">
</center>

## 仕様概要
Camera2 APIを使用して取得した背面カメラ画像をJPEGバイト列に変換  
取得解像度は1080p，送信解像度は1080p/720p/360p から選択できる  
このバイト列の先頭に情報を付加し，Socketを介して送信  
理論上の最大FPSは60 しかし変換処理等が重いためSDM845でも平均30に届かない程度と思われる

## 開発

### 環境
- Manjaro Linux (Linux 5.10.42-1)

### 使用ツール/SDK
- Android Studio
- Android SDK (Target API Level 30)
- Gradle

## To Build
Android StudioとAPI Level 30のAndroid SDKが導入されてれば問題ないはず  
Android Virtual Deviceでの動作も確認しているので，実機なしでもテスト可

## テスト端末
- Xperia XZ3 (SO-01L / Android 10)
    - 体感は普通に動作 スマホ側プレビューはそれなりに軽い．PC側はたまにカクつきあり

- Galaxy A7 (SM-A750C / Android 9)
    - やや重い スマホの性能不足がボトルネックになりPC側映像にもカクつき・遅延が増加

- Android Virtual Device (Pixel 3a)
    - PCのリソースに依る 挙動に問題はなし
