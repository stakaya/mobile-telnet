<?php
    // IP�A�h���X�擾
    if (isset($_SERVER['HTTP_X_FORWARDED_FOR'])) {
        $ip = $_SERVER['HTTP_X_FORWARDED_FOR'];
    } elseif (isset($_SERVER['REMOTE_ADDR'])) {
        $ip = $_SERVER['REMOTE_ADDR'];
    } elseif (isset($_SERVER['HTTP_CLIENT_IP'])) {
        $ip = $_SERVER['HTTP_CLIENT_IP'];
    } else {
        die('IP�A�h���X���s���ł��B');
    }

    // IP����
    $temp = split("\.", $ip);

    // IP�t�B���^
    if ($temp[0] == '210' && $temp[1] == '153' && $temp[2] == '84'
    ||  $temp[0] == '210' && $temp[1] == '136' && $temp[2] == '161'
    ||  $temp[0] == '210' && $temp[1] == '153' && $temp[2] == '86'
    ||  $temp[0] == '124' && $temp[1] == '146' && $temp[2] == '174'
    ||  $temp[0] == '124' && $temp[1] == '146' && $temp[2] == '175'
    ||  $temp[0] == '202' && $temp[1] == '229' && $temp[2] == '176'
    ||  $temp[0] == '202' && $temp[1] == '229' && $temp[2] == '177'
    ||  $temp[0] == '202' && $temp[1] == '229' && $temp[2] == '178'
    ||  $temp[0] == '210' && $temp[1] == '153' && $temp[2] == '87'
    ||  $temp[0] == '203' && $temp[1] == '138' && $temp[2] == '180'
    ||  $temp[0] == '203' && $temp[1] == '138' && $temp[2] == '181'
    ||  $temp[0] == '203' && $temp[1] == '138' && $temp[2] == '203'
    ||  $temp[0] == '127' && $temp[1] == '0' && $temp[2] == '0') {

        // �ꉞ���l�`�F�b�N
        if (0 > $temp[3] || $temp[3] > 255) {
            die($ip . ' IP�A�h���X���s���ł��B');
        }
    } else {
        die($ip . ' IP�A�h���X���s���ł��B');
    }
?>
