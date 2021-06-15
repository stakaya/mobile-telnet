import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Vector;

import javax.microedition.io.Connector;

import com.nttdocomo.io.HttpConnection;
import com.nttdocomo.ui.Canvas;
import com.nttdocomo.ui.Dialog;
import com.nttdocomo.ui.Display;
import com.nttdocomo.ui.Font;
import com.nttdocomo.ui.Graphics;
import com.nttdocomo.ui.IApplication;
import com.nttdocomo.ui.TextBox;
import com.nttdocomo.util.Phone;

/**
 * Telnet起動クラス
 *
 * @author takaya
 */
public class Telnet extends IApplication {

    /* (非 Javadoc)
     * @see com.nttdocomo.ui.IApplication#start()
     */
    public void start() {
        Display.setCurrent(new MainCanvas(this));
    }
}

/**
 * Telnet画面クラス
 *
 * @author takaya
 */
final class MainCanvas extends Canvas implements Runnable {

    /**
     * <code>args</code> アプリケーションの引数
     */
    public static final String[] args = IApplication.getCurrentApp().getArgs();

    /**
     * <code>SYSTEM_CGI</code> 実行CGI
     */
    public static final String SYSTEM_CGI = "operation.php";

    /**
     * <code>phpSession</code> PHPセッション
     */
    private static final String phpSession = getSystemDateTime();

    /**
     * <code>uniq</code> 発行済み日付
     */
    private static String uniq = "";

    /**
     * <code>path</code> PATH
     */
    private static String path = "";

    /**
     * <code>cmdHistory</code> コマンド
     */
    private static String cmdHistory = "cd ";

    /**
     * <code>nextCmd</code> 次のコマンド
     */
    private static String nextCmd = "";

    /**
     * <code>font</code> フォントサイズ
     */
    private static final int font = 12;

    /**
     * <code>cmdIndex</code> コマンド識別
     */
    private static int cmdIndex = 0;

    /**
     * <code>top</code> 表示座標高さ
     */
    private static int top = 0;

    /**
     * <code>left</code> 表示座標左
     */
    private static int left = 0;

    /**
     * <code>mode</code> モード:スクロール/コマンドヒストリ
     */
    private static int mode = 0;

    /**
     * <code>key</code> キーバッファ
     */
    private static int key = -1;

    /**
     * <code>timeout</code> タイムアウト識別
     */
    private static int timeout = 0;

    /**
     * <code>sending</code> 通信フラグ
     */
    private static boolean sending = false;

    /**
     * <code>cursor</code> カーソル点滅情報
     */
    private static boolean cursor = true;

    /**
     * <code>history</code> コマンドリストリ
     */
    private static Vector history = new Vector();

    /**
     * <code>console</code> コンソール表示用バッファ
     */
    private static Vector console = new Vector();

    /**
     * <code>editer</code> エディタ用バッファ
     */
    private static Vector editer = new Vector();

    /**
     * <code>conn</code> HTTPコネクション
     */
    private static HttpConnection conn = null;

    /**
     * <code>in</code> インプットストリーム
     */
    private static InputStream in = null;

    /**
     * <code>rec</code>インプットストリームリーダー
     */
    private static InputStreamReader rec = null;

    /**
     * <code>app</code> 親アプリ
     */
    private static IApplication app = null;

    /**
     * 通信サイズと通信ヘッダサイズ
     * DoJa2.0は5Kバイト  mova 504i及び504iSシリーズ
     * DoJa3.0は10Kバイト mova 505i及び505iS、506iシリーズ
     * DoJa3.5は80Kバイト FOMA 900iシリーズ
     * <code>DATE_SIZE    </code> 日付サイズ
     * <code>SIM_ID_SIZE  </code> SIMIDサイズ
     * <code>TERM_ID_SIZE </code> 製造番号サイズ
     * <code>PATH_SIZE    </code> PATHサイズ
     * <code>LENGTH_SIZE  </code> データレングスサイズ
     * <code>BAR_SIZE     </code> プログレスバーの位置合わせ用
     * <code>MAX_SEND_SIZE</code> 送信バッファサイズ
     */
    public static final int
        DATE_SIZE     =   14,
        SIM_ID_SIZE   =   20,
        TERM_ID_SIZE  =   15,
        PATH_SIZE     =    3,
        LENGTH_SIZE   =    6,
        BAR_SIZE      =    2,
        MAX_SEND_SIZE = 80000
                      - DATE_SIZE
                      - SIM_ID_SIZE
                      - TERM_ID_SIZE
                      - PATH_SIZE
                      - LENGTH_SIZE;

    /**
     * 携帯電話のID情報
     * <code>SIM_ID </code> SIM ID
     * <code>TERM_ID</code> TERM ID
     */
    public static final String
        SIM_ID  = Phone.getProperty(Phone.USER_ID),
        TERM_ID = Phone.getProperty(Phone.TERMINAL_ID);

    /**
     * コンストラクタ
     */
    MainCanvas(IApplication parents) {
        app = parents;
        setSoftLabel(SOFT_KEY_1, "EXIT");
        setSoftLabel(SOFT_KEY_2, "SCRL");
        setBackground(Graphics.getColorOfName(Graphics.BLACK));
        sendData(app.getSourceURL() + SYSTEM_CGI+ "?PHPSESSID=" + TERM_ID + phpSession, "cd .");
        new Thread(this).start();
    }

    /* (非 Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (true) {
            try {
                Thread.sleep(80);
            } catch (Exception e) {}

            // タイムアウトカウント
            if (sending) {
                timeout++;
            } else {
                timeout = 0;
            }

            // タイムアウトの場合
            if (timeout > 700) {
                try {
                    // コネクション切断
                    if (in != null) {
                        in.close();
                        in = null;
                    }
                    if (conn != null) {
                        conn.close();
                        conn = null;
                    }
                    if (rec != null) {
                        rec.close();
                        rec = null;
                    }
                } catch (Exception e) {
                    Dialog dialog = new Dialog(Dialog.DIALOG_ERROR, "エラー");
                    dialog.setText("通信切断に失敗しました\n");
                    dialog.show();
                }
                sending = false;
                timeout = 0;
            }

            // スクロールモード
            if (mode == 0 && key > -1) {
                switch(key) {

                // 左キー
                case Display.KEY_LEFT:
                    if (left != 0) left += font;
                    break;

                // 右キー
                case Display.KEY_RIGHT:
                    left -= font;
                    break;

                // 下キー
                case Display.KEY_DOWN:
                    if (top != 0) {
                        top += font;
                    }
                    break;

                // 上キー
                case Display.KEY_UP:
                    int line = console.size() - (getHeight() / font - 1) + top / font;
                    if (0 <= line && line < console.size()) {
                        top -= font;
                    }
                    break;
                }
            }
            this.repaint();
        }
    }

    /**
     * 現在日付と時間を返す
     * @return 現在日付(YYYYMMDDHHMMSS)
     */
    public static final String getSystemDateTime() {
        Calendar calendar = Calendar.getInstance();
        String temp = Long.toString(calendar.get(Calendar.YEAR)  * 10000000000L
                           + (calendar.get(Calendar.MONTH) + 1)  * 100000000
                           + calendar.get(Calendar.DAY_OF_MONTH) * 1000000
                           + calendar.get(Calendar.HOUR_OF_DAY)  * 10000
                           + calendar.get(Calendar.MINUTE)       * 100
                           + calendar.get(Calendar.SECOND));

        // 同じ日付の場合は再取得する
        if (temp.equals(uniq)) {
            temp = getSystemDateTime();
        }

        return uniq = temp;
    }

    /**
     * 通信用パッディング.
     * 指定レングスに文字列を加工する。
     * レングスの後ろをスペースで埋める。
     * @param data データ
     * @param length 指定レングス
     * @return 処理結果
     */
    public static String padding(final String data, final int length) {

        // nullの場合
        if (data == null) {
            return null;
        }

        // 指定サイズより大きい場合
        if (data.length() > length) {
            return data.substring(length);
        } else if (data.length() == length) {
            return data;
        }

        // 後ろにスペースを埋める
        String temp = data;
        for (int i = data.length(); i < length; i++) {
            temp += " ";
        }

        return temp;
    }

    /**
     * データ送信
     * @param url URL
     * @param data 送信データ
     * @return 処理結果
     */
    public static void send(final String url, final byte[] data) {
        OutputStream out = null;
        Dialog dialog = new Dialog(Dialog.DIALOG_ERROR, "エラー");

        try {
            // HTTP接続の準備
            conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
            conn.setRequestMethod(HttpConnection.POST);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // データを送信
            out = conn.openOutputStream();
            out.write(data);
        } catch (Exception e) {
            dialog.setText("HTTP接続例外発生\n");
            dialog.show();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                dialog.setText("HTTPデータ送信失敗\n");
                dialog.show();
            }
        }

        try {

            // サーバ接続
             conn.connect();
            if (conn.getResponseCode() != HttpConnection.HTTP_OK) {
                dialog.setText("HTTPデータ受信失敗\n");
                dialog.show();
            }

            // データ受信
            in = conn.openInputStream();
            rec = new InputStreamReader(in);
            StringBuffer temp = new StringBuffer();
            int buffer;

            for (int i = 0; (buffer = rec.read()) != -1; i++) {

                // 改行している場合
                if (buffer == 10) {
                    temp.append((char) buffer);
                    if (cmdHistory.startsWith("cd ")) {
                        path = temp.toString().trim();
                    } else {
                        console.addElement(temp.toString());
                    }
                    System.out.print(temp.toString());
                    temp.delete(0, temp.length());
                } else {
                    temp.append((char) buffer);
                }
            }

            System.out.print(temp.toString());
            if (!cmdHistory.startsWith("cd ")) {
                console.addElement(temp.toString());
            }
        } catch (Exception e) {
            dialog.setText("通信エラーが発生いたしました\n");
            dialog.show();
        } finally {
            try {
                // コネクション切断
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
                if (rec != null) {
                    rec.close();
                    rec = null;
                }
            } catch (Exception e) {
                dialog.setText("通信切断に失敗しました\n");
                dialog.show();
            }
        }
    }

    /**
     * データ送信処理
     * @param url URL
     * @param type 識別子
     * @param command コマンド
     * @return 処理結果
     */
    public static void sendData(final String url, final String command) {
        byte[] packet = new byte[MAX_SEND_SIZE];
        String header = null;
        String date = getSystemDateTime();
        ByteArrayOutputStream out = null;
        int length = 0;
        int i = 0;

        try {
            // コマンドデータ送信
            byte[] sendCommand = command.getBytes();
            length = sendCommand.length;

            // コマンドデータ送信
            header = padding(date,                     DATE_SIZE)
                   + padding(SIM_ID,                   SIM_ID_SIZE)
                   + padding(TERM_ID,                  TERM_ID_SIZE)
                   + padding(Integer.toString(
                             path.getBytes().length),  PATH_SIZE)
                   + padding(Integer.toString(length), LENGTH_SIZE);

            // データ編集
            byte[] sendHeader = header.getBytes();
            out = new ByteArrayOutputStream(sendHeader.length + length);
            out.write(sendHeader);
            out.write(path.getBytes());
            out.write(sendCommand);
            out.close();
            // データ送信
            send(url, out.toByteArray());
        } catch (Exception e) {
            Dialog dialog = new Dialog(Dialog.DIALOG_ERROR, "エラー");
            dialog.setText("通信切断に失敗しました\n");
            dialog.show();
        }
    }

    /* (非 Javadoc)
     * @see com.nttdocomo.ui.Canvas#paint(com.nttdocomo.ui.Graphics)
     */
    public void paint(Graphics g) {
        g.lock();
        g.clearRect(0, 0, Display.getWidth(), Display.getHeight());
        g.setColor(Graphics.getColorOfName(Graphics.WHITE));
        g.setFont(Font.getFont(Font.FACE_SYSTEM|Font.STYLE_PLAIN|Font.SIZE_TINY));

        int line = 0;
        if (console.size() > getHeight() / font - 1) {
            line = console.size() - (getHeight() / font - 1) + top / font;

            if (line < 0) {
                line = 0;
            } else if (line >= console.size()) {
                line = console.size() - 1;
            }
        }

        int offset = font;
        for (int i = line; i < console.size(); i++, offset += font) {
            g.drawString((String) console.elementAt(i), left, offset);
        }

        if (cursor = !cursor) {
            g.drawString(path + ">" + nextCmd + "_", left, offset);
        } else {
            g.drawString(path + ">" + nextCmd, left, offset);
        }
        g.unlock(true);
    }

    /**
     * IMEイベント
     * @see com.nttdocomo.ui.Canvas#processIMEEvent(int, java.lang.String)
     */
    public void processIMEEvent(final int type, final String text) {
        if (type == IME_COMMITTED) {
            if (text.trim().length() == 0) {
                return;
            } else {
                cmdHistory = text;
            }

            // EXITなら終了
            if (text.trim().toLowerCase().equals("exit")) {
                (IApplication.getCurrentApp()).terminate();
            }

            console.addElement(path + ">" + text);
            sending = true;
            sendData(app.getSourceURL() + SYSTEM_CGI + "?PHPSESSID=" + TERM_ID + phpSession, text);
            sending = false;
            history.addElement(text);
            cmdIndex = history.size();
            nextCmd = "";
            top = 0;
            this.repaint();
        }
    }

    /**
     * キーイベント
     * @see com.nttdocomo.ui.Canvas#processEvent(int, int)
     */
    public void processEvent(final int type, final int param) {
        if (type == Display.KEY_PRESSED_EVENT) {
            // キーバッファに記録
            key = param;
        }

        if (type == Display.KEY_RELEASED_EVENT) {
            key = -1;

            // コマンドモード
            if (mode == 1) {
                switch(param) {

                // 下キー
                case Display.KEY_DOWN:
                	if (history.size() > cmdIndex +1 && history.size() > 0) {
                   		nextCmd = (String) history.elementAt(++cmdIndex);
                	}
                	break;

                // 上キー
                case Display.KEY_UP:
                	if (0 < cmdIndex && history.size() > cmdIndex -1 && history.size() > 0) {
                   		nextCmd = (String) history.elementAt(--cmdIndex);
                	}
	                break;
                }
            }

            switch(param) {

            // 数字キーで入力補正
            case Display.KEY_1:
            	if (args.length >= Display.KEY_1)
                    nextCmd = args[Display.KEY_1 -1];
                    break;
            case Display.KEY_2:
            	if (args.length >= Display.KEY_2)
                    nextCmd = args[Display.KEY_2 -1];
                    break;
            case Display.KEY_3:
        	    if (args.length >= Display.KEY_3)
                    nextCmd = args[Display.KEY_3 -1];
                    break;
            case Display.KEY_4:
        	    if (args.length >= Display.KEY_4)
                    nextCmd = args[Display.KEY_4 -1];
                    break;
            case Display.KEY_5:
        	    if (args.length >= Display.KEY_5)
                    nextCmd = args[Display.KEY_5 -1];
                    break;
            case Display.KEY_6:
        	    if (args.length >= Display.KEY_6)
                    nextCmd = args[Display.KEY_6 -1];
                    break;
            case Display.KEY_7:
        	    if (args.length >= Display.KEY_7)
                    nextCmd = args[Display.KEY_7 -1];
                    break;
            case Display.KEY_8:
        	    if (args.length >= Display.KEY_8)
                    nextCmd = args[Display.KEY_8 -1];
                    break;
            case Display.KEY_9:
        	    if (args.length >= Display.KEY_9)
                    nextCmd = args[Display.KEY_9 -1];
                    break;

            // ソフトキー1
            case Display.KEY_SOFT1:
                (IApplication.getCurrentApp()).terminate();
                break;

            // 決定キー
            case Display.KEY_SELECT:
                imeOn(nextCmd, TextBox.DISPLAY_ANY, TextBox.ALPHA);
                break;

            // ソフトキー2
            case Display.KEY_SOFT2:
                if (mode == 0) {
                    mode = 1;
                    top  = 0;
                    left = 0;
                    setSoftLabel(SOFT_KEY_2, "COMD");
                } else {
                    mode = 0;
                    setSoftLabel(SOFT_KEY_2, "SCRL");
                }
                this.repaint();
                break;
            }
        }
    }
}
