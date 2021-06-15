<?php
    // IPフィルタ
	include_once('ipfilter.php');

	// ユーザエージェント取得
	$ua = $_SERVER['HTTP_USER_AGENT'];

	// 端末番号取得
	if (!strpos($ua, ';')) {

	    // 前の部分を切り捨てる
	    $trem = substr(strstr($ua, 'ser'), 3);
	} else {
	    // セミコロンで区切る
	    $trem = split(';', $ua);

	    // 先頭の'ser'と取り除く
	    $trem = substr($trem[count($trem) -2], 3);
	}

    // SIM-ID取得、セミコロンで区切る
    $sim = split(';', $ua);

    // 後ろの')'と先頭の'icc'と取り除く
    $sim = substr(trim($sim[count($sim) -1], ')'), 3);

	// プロトコル判定
	if (!strpos($_SERVER['SERVER_PROTOCOL'], 'HTTPS')) {
		$url = 'http://';
	} else {
		$url = 'https://';
	}

	$url = $url
         . $_SERVER['SERVER_NAME']
         . dirname($_SERVER['PHP_SELF'])
         . '/MobileTelnet.jar';

	$week = array('Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat');
	$month = array('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec');
	$modified = $week[date('w', time())] . ', '
	          . date('d', time()) . ' '
	          . $month[date('n', time()) -1]
	          . date(' Y H:i:s', time());
	$appSize = filesize('MobileTelnet.jar');

    // アプリ名
	if (isset($_POST['appname']) && strlen($_POST['appname']) > 0) {
		$appname = $_POST['appname'];
	} else {
		$appname = 'モバターム';
	}

	// コマンド登録
	$param = '';
	if (isset($_POST['com1']) && strlen($_POST['com1']) > 0) {
		$param .= $_POST['com1'] . ' ';
	}
	if (isset($_POST['com2']) && strlen($_POST['com2']) > 0) {
		$param .= $_POST['com2'] . ' ';
	}
	if (isset($_POST['com3']) && strlen($_POST['com3']) > 0) {
		$param .= $_POST['com3'] . ' ';
	}
	if (isset($_POST['com4']) && strlen($_POST['com4']) > 0) {
		$param .= $_POST['com4'] . ' ';
	}
	if (isset($_POST['com5']) && strlen($_POST['com5']) > 0) {
		$param .= $_POST['com5'] . ' ';
	}
	if (isset($_POST['com6']) && strlen($_POST['com6']) > 0) {
		$param .= $_POST['com6'] . ' ';
	}
	if (isset($_POST['com7']) && strlen($_POST['com7']) > 0) {
		$param .= $_POST['com7'] . ' ';
	}
	if (isset($_POST['com8']) && strlen($_POST['com8']) > 0) {
		$param .= $_POST['com8'] . ' ';
	}
	if (isset($_POST['com9']) && strlen($_POST['com9']) > 0) {
		$param .= $_POST['com9'] . ' ';
	}

	// 画面サイズ登録
	if (isset($_POST['width']) && isset($_POST['height'])
    && preg_match("/^[0-9]+$/", $_POST['width'])
    && preg_match("/^[0-9]+$/", $_POST['height'])) {
		$drawArea = "\nDrawArea = "
                  . $_POST['width']
                  . 'x'
                  . $_POST['height'];
	} else {
		$drawArea = '';
	}

$jam =<<<JAM_FILE
PackageURL = {$url}
AppSize = {$appSize}
AppName = {$appname}{$drawArea}
AppParam = {$param}
AppClass = Telnet
UseNetwork = http
AccessUserInfo = yes
LastModified = {$modified}
GetUtn = terminalid,userid
AppIcon = icon.gif
JAM_FILE;

	// ファイル出力
	$fp = fopen('jam.php', 'w');

	$jam = '<?php '
	     . "unlink('jam.php');"
	     . "unlink('download.php');"
	     . "unlink('index.html');"
	     . "unlink('operation_tmp.php');"
         . ' ?>'
         . $jam
         . "\n";

	fwrite($fp, $jam);
	fclose($fp);

	$fp = fopen('operation_tmp.php', 'r');
	$operation = fread($fp, filesize('operation_tmp.php'));
	fclose($fp);

	$operation = "<?php\n"
	           . "define('SIM_ID', '$sim');\n"
	           . "define('TREM_ID', '$trem');\n"
               . "?>"
               . $operation;

	$fp = fopen('operation.php', 'w');
	fwrite($fp, $operation);
	fclose($fp);
?>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=Shift-JIS" />
  <title>M-Term</title>
</head>
<body>
<object declare id="MobileTelnet"
        data="jam.php"
        type="application/x-jam">
</object>
<a ijam="#MobileTelnet" href="index.html">DOWNLOAD</a>
</body>
</html>
