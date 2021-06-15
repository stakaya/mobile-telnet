<?php
    // IPフィルタ
    include_once('ipfilter.php');

    // 言語設定
    mb_language('Japanese');

    // ユーザエージェント取得
    if (!preg_match('/^DoCoMo/', $_SERVER['HTTP_USER_AGENT'])) {
        die($_SERVER['HTTP_USER_AGENT'] . ' ドコモ携帯以外です。');
    } else {
        ini_set('session.use_trans_sid', '1');
    }

    // セッションスタート
    session_start();

    if (!isset($_SESSION['SESSION'])) {
        $_SESSION['SESSION'] = 'on';

        // 初回の起動はパスを返す
        if (PHP_OS == 'WIN32' || PHP_OS == 'WINNT') {
            passthru('cd');
        } else {
            passthru('pwd');
        }
        return;
    }

    // POST情報取得
    if (isset($HTTP_RAW_POST_DATA)) {
        $stdin = $HTTP_RAW_POST_DATA;
    } else {
        $fp = fopen('php://input', 'r');
        if(!$fp) {
            die('標準入力でエラーが発生しました。');
        }

        $stdin = '';
        while (!feof($fp)) {
            $stdin .= fgets($fp);
        }
        fclose($fp);
    }

    // レングスチェック
    if (strlen($stdin) < 58) {
        return;
    }

    // パラメータレングス
    $pos = array(14,  // 日付サイズ
                 20,  // SIMIDサイズ
                 15,  // 製造番号サイズ
                  3,  // PATHサイズ
                  6); // データレングスサイズ

    // ヘッダ情報取得
    $date   = substr($stdin, 0,     $pos[$i=0]); $offset  = $pos[$i++];
    $simid  = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];
    $termid = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];
    $size   = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];
    $length = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];

    // スペースを削除
    $size   = trim($size);
    $length = trim($length);

    // パス取得
    $path = substr($stdin, $offset, $size);

    // データがある場合
    if ($length > 0) {
        $command = substr($stdin, $offset + $size, $length);
    } else {
        return;
    }

    // cdコマンドの場合ディレクトリを出力
    if (substr(trim($command), 0, 3) == 'cd ') {
        $temp = trim(substr($command, 3), " \t\n\r");
        // Windowsの場合
        if (PHP_OS == 'WIN32' || PHP_OS == 'WINNT') {
            // 絶対・相対を判別
            if (substr($temp, 1, 1) == ':') {
                @chdir($temp);
            } else {
                @chdir($path . DIRECTORY_SEPARATOR . $temp);
            }
            passthru('cd');
        } else {
            // 絶対・相対を判別
            if (substr($temp, 0, 1) == DIRECTORY_SEPARATOR) {
                @chdir($temp);
            } else {
                @chdir($path . DIRECTORY_SEPARATOR . $temp);
            }
            passthru('pwd');
        }
    } else {
        // パス指定が無い場合
        if (empty($path)) {
            $path = getcwd();
        }

        // プロセスオープン
        $process = proc_open($command, array(
            0 => array('pipe', 'r'),
            1 => array('pipe', 'w'),
            2 => array('pipe', 'w')
            ), $pipes, $path);

        // プロセス起動失敗
        if (!$process) {
            return;
        }

        // コマンド分解
        $temp = split("\n", $command);
        for ($i = 1; $i < count($temp); $i++){
            fwrite($pipes[0], $temp[$i]);
        }
        fclose($pipes[0]);

        // 固まるの防止
        stream_set_blocking($pipes[1], 0);
        stream_set_blocking($pipes[2], 0);
        set_time_limit(60);

        // パイプが読み出し可能な間
        while (!feof($pipes[1]) || !feof($pipes[2])) {

            $in = array($pipes[1], $pipes[2]);
            $ex = $out = null;
            $result = stream_select($in, $out, $ex, 5);

            // 異常・タイムアウトした場合
            if ($result === false || $result === 0) {
                proc_terminate($process);
                die('コマンドがタイムアウトしました。');
                break;
            } elseif ($result > 0) {
                // データ読み出し
                foreach ($in as $temp) {
                    while (!feof($temp)) {
                        print mb_convert_encoding(fgets($temp), 'SJIS', 'auto');
                    }
                }
            }
        }

        // プロセスクローズ
        fclose($pipes[1]);
        fclose($pipes[2]);
        proc_close($process);
    }
?>
