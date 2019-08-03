import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


//채팅방을 관리하는 서버측 클래스
public class Hserver {

    public static final int ServerPort = 8000;


    void initNet() throws Exception {


        //서버소켓 생성. 클라이언트 접속을 기다림
        ServerSocket serverSocket = new ServerSocket(ServerPort);
        consoleLog("Listening to 8000..");

        while (true) {

            Socket socket = serverSocket.accept();

            consoleLog(socket.getInetAddress()+"is connected");

        }

    }



    //서버 콘솔에 로그 남기기
    public void consoleLog(String message){
        System.out.println(message);
    }



    public static void main(String args[]) throws Exception {

        Hserver server = new Hserver();

        server.initNet();

    }

}
