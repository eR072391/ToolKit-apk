# Tool Kit
コード簡略化のため全ての操作はダイアログを使用しています。<br>
<機能><br>
・「Exif情報取得」...画像からexif情報を抽出します。<br>
・「Domain -> IP」...ドメイン名からIPアドレスを正引きします。<br>
・「インターネット接続確認」...インターネットに接続可能か確認します。自分のグローバルIPアドレスも取得します。<br>
・「WHOIS」...ドメイン名からwhois情報を取得します。<br>
・「Port Scan」...ドメイン名と(1つ)ポート番号を指定して、疎通確認を行います。<br>
・「TCP Connect」...Socketを使用してTCP接続を行います。<br>

## Usage
見た目はこんな感じです。<br>
![](images/1.png)
<br>
試しに「WHOIS」機能を使ってみます。<br>
ダイアログが表示され、ドメイン名を入力すると、結果が表示されます。<br>
![](images/2.png)![](images/3.png)
<br>
次に、「TCP Connect」機能は、Socketを使用してTCP接続を行っているので、<br>
好きなリクエストを送信することができます。<br>
![](images/4.png)![](images/5.png)
