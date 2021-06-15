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
 * Telnet�N���N���X
 *
 * @author takaya
 */
public class Telnet extends IApplication {

    /* (�� Javadoc)
     * @see com.nttdocomo.ui.IApplication#start()
     */
    public void start() {
        Display.setCurrent(new MainCanvas(this));
    }
}

/**
 * Telnet��ʃN���X
 *
 * @author takaya
 */
final class MainCanvas extends Canvas implements Runnable {

    /**
     * <code>args</code> �A�v���P�[�V�����̈���
     */
    public static final String[] args = IApplication.getCurrentApp().getArgs();

    /**
     * <code>SYSTEM_CGI</code> ���sCGI
     */
    public static final String SYSTEM_CGI = "operation.php";

    /**
     * <code>phpSession</code> PHP�Z�b�V����
     */
    private static final String phpSession = getSystemDateTime();

    /**
     * <code>uniq</code> ���s�ςݓ��t
     */
    private static String uniq = "";

    /**
     * <code>path</code> PATH
     */
    private static String path = "";

    /**
     * <code>cmdHistory</code> �R�}���h
     */
    private static String cmdHistory = "cd ";

    /**
     * <code>nextCmd</code> ���̃R�}���h
     */
    private static String nextCmd = "";

    /**
     * <code>font</code> �t�H���g�T�C�Y
     */
    private static final int font = 12;

    /**
     * <code>cmdIndex</code> �R�}���h����
     */
    private static int cmdIndex = 0;

    /**
     * <code>top</code> �\�����W����
     */
    private static int top = 0;

    /**
     * <code>left</code> �\�����W��
     */
    private static int left = 0;

    /**
     * <code>mode</code> ���[�h:�X�N���[��/�R�}���h�q�X�g��
     */
    private static int mode = 0;

    /**
     * <code>key</code> �L�[�o�b�t�@
     */
    private static int key = -1;

    /**
     * <code>timeout</code> �^�C���A�E�g����
     */
    private static int timeout = 0;

    /**
     * <code>sending</code> �ʐM�t���O
     */
    private static boolean sending = false;

    /**
     * <code>cursor</code> �J�[�\���_�ŏ��
     */
    private static boolean cursor = true;

    /**
     * <code>history</code> �R�}���h���X�g��
     */
    private static Vector history = new Vector();

    /**
     * <code>console</code> �R���\�[���\���p�o�b�t�@
     */
    private static Vector console = new Vector();

    /**
     * <code>editer</code> �G�f�B�^�p�o�b�t�@
     */
    private static Vector editer = new Vector();

    /**
     * <code>conn</code> HTTP�R�l�N�V����
     */
    private static HttpConnection conn = null;

    /**
     * <code>in</code> �C���v�b�g�X�g���[��
     */
    private static InputStream in = null;

    /**
     * <code>rec</code>�C���v�b�g�X�g���[�����[�_�[
     */
    private static InputStreamReader rec = null;

    /**
     * <code>app</code> �e�A�v��
     */
    private static IApplication app = null;

    /**
     * �ʐM�T�C�Y�ƒʐM�w�b�_�T�C�Y
     * DoJa2.0��5K�o�C�g  mova 504i�y��504iS�V���[�Y
     * DoJa3.0��10K�o�C�g mova 505i�y��505iS�A506i�V���[�Y
     * DoJa3.5��80K�o�C�g FOMA 900i�V���[�Y
     * <code>DATE_SIZE    </code> ���t�T�C�Y
     * <code>SIM_ID_SIZE  </code> SIMID�T�C�Y
     * <code>TERM_ID_SIZE </code> �����ԍ��T�C�Y
     * <code>PATH_SIZE    </code> PATH�T�C�Y
     * <code>LENGTH_SIZE  </code> �f�[�^�����O�X�T�C�Y
     * <code>BAR_SIZE     </code> �v���O���X�o�[�̈ʒu���킹�p
     * <code>MAX_SEND_SIZE</code> ���M�o�b�t�@�T�C�Y
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
     * �g�ѓd�b��ID���
     * <code>SIM_ID </code> SIM ID
     * <code>TERM_ID</code> TERM ID
     */
    public static final String
        SIM_ID  = Phone.getProperty(Phone.USER_ID),
        TERM_ID = Phone.getProperty(Phone.TERMINAL_ID);

    /**
     * �R���X�g���N�^
     */
    MainCanvas(IApplication parents) {
        app = parents;
        setSoftLabel(SOFT_KEY_1, "EXIT");
        setSoftLabel(SOFT_KEY_2, "SCRL");
        setBackground(Graphics.getColorOfName(Graphics.BLACK));
        sendData(app.getSourceURL() + SYSTEM_CGI+ "?PHPSESSID=" + TERM_ID + phpSession, "cd .");
        new Thread(this).start();
    }

    /* (�� Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (true) {
            try {
                Thread.sleep(80);
            } catch (Exception e) {}

            // �^�C���A�E�g�J�E���g
            if (sending) {
                timeout++;
            } else {
                timeout = 0;
            }

            // �^�C���A�E�g�̏ꍇ
            if (timeout > 700) {
                try {
                    // �R�l�N�V�����ؒf
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
                    Dialog dialog = new Dialog(Dialog.DIALOG_ERROR, "�G���[");
                    dialog.setText("�ʐM�ؒf�Ɏ��s���܂���\n");
                    dialog.show();
                }
                sending = false;
                timeout = 0;
            }

            // �X�N���[�����[�h
            if (mode == 0 && key > -1) {
                switch(key) {

                // ���L�[
                case Display.KEY_LEFT:
                    if (left != 0) left += font;
                    break;

                // �E�L�[
                case Display.KEY_RIGHT:
                    left -= font;
                    break;

                // ���L�[
                case Display.KEY_DOWN:
                    if (top != 0) {
                        top += font;
                    }
                    break;

                // ��L�[
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
     * ���ݓ��t�Ǝ��Ԃ�Ԃ�
     * @return ���ݓ��t(YYYYMMDDHHMMSS)
     */
    public static final String getSystemDateTime() {
        Calendar calendar = Calendar.getInstance();
        String temp = Long.toString(calendar.get(Calendar.YEAR)  * 10000000000L
                           + (calendar.get(Calendar.MONTH) + 1)  * 100000000
                           + calendar.get(Calendar.DAY_OF_MONTH) * 1000000
                           + calendar.get(Calendar.HOUR_OF_DAY)  * 10000
                           + calendar.get(Calendar.MINUTE)       * 100
                           + calendar.get(Calendar.SECOND));

        // �������t�̏ꍇ�͍Ď擾����
        if (temp.equals(uniq)) {
            temp = getSystemDateTime();
        }

        return uniq = temp;
    }

    /**
     * �ʐM�p�p�b�f�B���O.
     * �w�背���O�X�ɕ���������H����B
     * �����O�X�̌����X�y�[�X�Ŗ��߂�B
     * @param data �f�[�^
     * @param length �w�背���O�X
     * @return ��������
     */
    public static String padding(final String data, final int length) {

        // null�̏ꍇ
        if (data == null) {
            return null;
        }

        // �w��T�C�Y���傫���ꍇ
        if (data.length() > length) {
            return data.substring(length);
        } else if (data.length() == length) {
            return data;
        }

        // ���ɃX�y�[�X�𖄂߂�
        String temp = data;
        for (int i = data.length(); i < length; i++) {
            temp += " ";
        }

        return temp;
    }

    /**
     * �f�[�^���M
     * @param url URL
     * @param data ���M�f�[�^
     * @return ��������
     */
    public static void send(final String url, final byte[] data) {
        OutputStream out = null;
        Dialog dialog = new Dialog(Dialog.DIALOG_ERROR, "�G���[");

        try {
            // HTTP�ڑ��̏���
            conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
            conn.setRequestMethod(HttpConnection.POST);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // �f�[�^�𑗐M
            out = conn.openOutputStream();
            out.write(data);
        } catch (Exception e) {
            dialog.setText("HTTP�ڑ���O����\n");
            dialog.show();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                dialog.setText("HTTP�f�[�^���M���s\n");
                dialog.show();
            }
        }

        try {

            // �T�[�o�ڑ�
             conn.connect();
            if (conn.getResponseCode() != HttpConnection.HTTP_OK) {
                dialog.setText("HTTP�f�[�^��M���s\n");
                dialog.show();
            }

            // �f�[�^��M
            in = conn.openInputStream();
            rec = new InputStreamReader(in);
            StringBuffer temp = new StringBuffer();
            int buffer;

            for (int i = 0; (buffer = rec.read()) != -1; i++) {

                // ���s���Ă���ꍇ
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
            dialog.setText("�ʐM�G���[�������������܂���\n");
            dialog.show();
        } finally {
            try {
                // �R�l�N�V�����ؒf
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
                dialog.setText("�ʐM�ؒf�Ɏ��s���܂���\n");
                dialog.show();
            }
        }
    }

    /**
     * �f�[�^���M����
     * @param url URL
     * @param type ���ʎq
     * @param command �R�}���h
     * @return ��������
     */
    public static void sendData(final String url, final String command) {
        byte[] packet = new byte[MAX_SEND_SIZE];
        String header = null;
        String date = getSystemDateTime();
        ByteArrayOutputStream out = null;
        int length = 0;
        int i = 0;

        try {
            // �R�}���h�f�[�^���M
            byte[] sendCommand = command.getBytes();
            length = sendCommand.length;

            // �R�}���h�f�[�^���M
            header = padding(date,                     DATE_SIZE)
                   + padding(SIM_ID,                   SIM_ID_SIZE)
                   + padding(TERM_ID,                  TERM_ID_SIZE)
                   + padding(Integer.toString(
                             path.getBytes().length),  PATH_SIZE)
                   + padding(Integer.toString(length), LENGTH_SIZE);

            // �f�[�^�ҏW
            byte[] sendHeader = header.getBytes();
            out = new ByteArrayOutputStream(sendHeader.length + length);
            out.write(sendHeader);
            out.write(path.getBytes());
            out.write(sendCommand);
            out.close();
            // �f�[�^���M
            send(url, out.toByteArray());
        } catch (Exception e) {
            Dialog dialog = new Dialog(Dialog.DIALOG_ERROR, "�G���[");
            dialog.setText("�ʐM�ؒf�Ɏ��s���܂���\n");
            dialog.show();
        }
    }

    /* (�� Javadoc)
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
     * IME�C�x���g
     * @see com.nttdocomo.ui.Canvas#processIMEEvent(int, java.lang.String)
     */
    public void processIMEEvent(final int type, final String text) {
        if (type == IME_COMMITTED) {
            if (text.trim().length() == 0) {
                return;
            } else {
                cmdHistory = text;
            }

            // EXIT�Ȃ�I��
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
     * �L�[�C�x���g
     * @see com.nttdocomo.ui.Canvas#processEvent(int, int)
     */
    public void processEvent(final int type, final int param) {
        if (type == Display.KEY_PRESSED_EVENT) {
            // �L�[�o�b�t�@�ɋL�^
            key = param;
        }

        if (type == Display.KEY_RELEASED_EVENT) {
            key = -1;

            // �R�}���h���[�h
            if (mode == 1) {
                switch(param) {

                // ���L�[
                case Display.KEY_DOWN:
                	if (history.size() > cmdIndex +1 && history.size() > 0) {
                   		nextCmd = (String) history.elementAt(++cmdIndex);
                	}
                	break;

                // ��L�[
                case Display.KEY_UP:
                	if (0 < cmdIndex && history.size() > cmdIndex -1 && history.size() > 0) {
                   		nextCmd = (String) history.elementAt(--cmdIndex);
                	}
	                break;
                }
            }

            switch(param) {

            // �����L�[�œ��͕␳
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

            // �\�t�g�L�[1
            case Display.KEY_SOFT1:
                (IApplication.getCurrentApp()).terminate();
                break;

            // ����L�[
            case Display.KEY_SELECT:
                imeOn(nextCmd, TextBox.DISPLAY_ANY, TextBox.ALPHA);
                break;

            // �\�t�g�L�[2
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
