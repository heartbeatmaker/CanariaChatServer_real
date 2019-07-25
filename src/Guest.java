
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;


//클라이언트와의 데이터 송수신을 처리하는 서버측 클래스
class Guest extends Thread {

    int id;
    String username;
    String roomName;
    int roomId;
    Server server;
    Socket socket;
    BufferedReader reader;
    BufferedWriter writer;
    ConnectDB connect;


    Guest(Server server, Socket socket, ConnectDB connect) throws Exception {

        this.server = server;
        this.socket = socket;

        this.connect = connect;

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

                    //새로운 방 개설해달라는 메시지
                    //new_room/userId/username/friendInfo(=jsonArray.toString)
                    case "new_room":
                        id = Integer.valueOf(array[1]); //사용자 id
                        username = array[2]; //사용자 username
                        String friendInfo_string = array[3]; //참여자id;username;id;username ...

                        System.out.println(username+" created a room.");
                        System.out.println("FriendInfo="+friendInfo_string);

                        //db에 방 정보를 저장하고, 방 id를 얻는다
                        roomId = connect.insertRoom();
                        System.out.println("roomId="+roomId);

                        //방을 개설하고, 현 사용자(=방 개설자)를 그 방에 join 시킨다
                        server.addRoom(roomId, this);

                        //나머지 참여자(=초대된 사람들)들은 guest 객체를 저장할 수 없다(로비 사용자이므로)
                        //따라서 그들의 id값만 다른 리스트에 저장해 놓는다
                        String[] friendId_array = friendInfo_string.split(";");
                        System.out.println("friendId_array="+ Arrays.asList(friendId_array));


                        int friend_id = 10000;
                        String friend_username = "";

                        for(int i=0; i<friendId_array.length; i++){
                            if(i%2==0){
                                friend_id = Integer.valueOf(friendId_array[i]);
                            }else{
                                friend_username = friendId_array[i];
                            }

                            if(i%2==1){
                                System.out.println("id="+friend_id+" / username="+friend_username);
                                server.addGuestIdToRoom(roomId, friend_id, friend_username);
                            }
                        }

                        sendMsg("room_created/"+roomId);
                        server.broadcastRoomInfo(roomId, "room"+roomId);
                        server.broadcastToRoom(roomId, "serverMsg/A room is created.");

                        break;
                    // 기존 방에 입장했을 때
                    // enter/userId/username/roomId/roomName
                    case "enter":
                        id = Integer.valueOf(array[1]); //사용자 id
                        username = array[2]; //사용자 username
                        roomId = Integer.valueOf(array[3]);
                        roomName = array[4];

                        server.consoleLog(id+" entered "+roomName+" again.");

                        //방에 입장시킨다
                        server.enterExistingRoom(roomId, roomName, this);

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

