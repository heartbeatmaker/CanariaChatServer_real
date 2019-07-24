import java.io.*;
import java.net.Socket;


//클라이언트와의 데이터 송수신을 처리하는 서버측 클래스
class Guest extends Thread {

    String id;
    String username;
    String roomName;
    int roomId;
    Server server;
    Socket socket;
    BufferedReader reader;
    BufferedWriter writer;


    Guest(Server server, Socket socket) throws Exception {

        this.server = server;
        this.socket = socket;

        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        reader = new BufferedReader(isr);

        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        writer = new BufferedWriter(osw);
    }


    public void run() {
        try {
            while (true) {

                String line = reader.readLine();
                System.out.println("message from client: "+line);
                String array[] = line.split("/");

                switch (array[0]) {

                    //enter/id/username
                    case "connect":
                        id = array[1]; //사용자 id
                        username = array[2]; //사용자 username
                        roomId = Integer.valueOf(array[3]);
                        roomName = array[4];

                        server.consoleLog(id+" is connected");

                        //이 사용자의 id, 닉네임, socket을 저장한다
                        // => Server 클래스에서 addGuest()메소드로 이 guest를 목록에 추가함

                        //해당 이름의 방이 있는지 확인하고, 방을 만들거나 입장시킨다
                        server.checkIfRoomExists(roomId, roomName, this);

                        break;

                    //client.sendMsg("msg/메시지);
                    case "msg":
                        try {
                            String message = array[1];

                            //본인이 보낸 메시지를 전달하기 때문에, id를 붙일 필요 없다
                            sendMsg("myMsg/"+message);
                            //누가 보냈는지 알아야 하므로, 사용자의 id와 닉네임을 붙여서 보낸다
                            server.broadcastToRoomExceptMe(roomId, "msg/"+id+"/"+username+"/"+message, id);
                        }catch(Exception e){
                            System.out.println("msg error: (no room?!)"+e);
                        }
                        break;


                    case "closeRoom": //사용자가 방을 닫았을 때(inactive 상태)
                        //네트워크 문제로 소켓이 끊긴 상태일 때도 closeRoom 으로 간주한다

                        System.out.println(username+" is now inactive at room ("+roomId+")");
                        server.broadcastToRoomExceptMe(roomId, "inactive/"+username+" is not reading messages.", id);

                        break;
//                    case "disconnect": //사용자가 방에서 나갔을 때
//
//                        //해당 사용자를 방 목록 / 전체 사용자 목록에서 제거한다
//                        server.removeGuestFromRoom(roomId, this);
//                        server.removeGuestFromLobby(this);
//
//                        break;

//                    case "roomout" :
//                        server.removeGuestFromRoom(array[2], this);
//                        server.removeRoom(array[2]); //방에 아무도 없으면 방을 삭제
//                        server.broadcastGuestlistAtLobby();
//                        break;
                }

            }
        } catch (Exception e) {
            System.out.println("readLine() error:");
            e.printStackTrace();

            try {
                //소켓이 끊기면, readline 시 null이 뜬다 -> NullPointerException 발생
                //-> 해당 사용자를 inactive 상태로 간주한다
                server.broadcastToRoomExceptMe(roomId, "inactive/"+username+" is not reading messages.", id);

//                server.removeGuestFromRoom(roomId, this);
//                server.removeGuestFromLobby(this);
            } catch (Exception e1) {
                System.out.println("removeGuest() error:"+ e1);
            }
        }
    }

    //이 사용자에게 메시지 보내기
    public void sendMsg(String msg) throws Exception {

        writer.write(msg + "\n");
        writer.flush();
    }

}

