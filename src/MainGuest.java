import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


//클라이언트와의 데이터 송수신을 처리하는 서버측 클래스
class MainGuest extends Thread {

    int id;
    String username;

//    String roomName;
//    int roomId;

    MainServer server;
    Socket socket;
    BufferedReader reader;
    BufferedWriter writer;
    ConnectDB connect;


    MainGuest(MainServer server, Socket socket, ConnectDB connect) throws Exception {

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

                    //사용자가 접속하였다는 메시지
                    case "connect":
                        id = Integer.valueOf(array[1]); //사용자 id
                        username = array[2]; //사용자 username
                        String myRoom_id = array[3]; //사용자가 가입한 방id 목록

                        String [] myRoom_id_array = myRoom_id.split(";");

                        //기존에 저장되어있던 socket을 현재 것으로 바꾼다
                        for(int i=0; i<myRoom_id_array.length; i++){
                            int roomId = Integer.valueOf(myRoom_id_array[i]);
                            boolean isMember=false;

                            if(server.roomHashMap.containsKey(roomId)){
                                System.out.println("room "+roomId+" exists at server.");

                                ArrayList<MainGuest> guestList_room = server.roomHashMap.get(roomId);
                                for(int k=0; k<guestList_room.size(); k++){
                                    MainGuest anonymous_guest = guestList_room.get(k);
                                    if(anonymous_guest.id == id){

                                        anonymous_guest.socket = socket;
                                        anonymous_guest.reader = reader;
                                        anonymous_guest.writer = writer;

                                        System.out.println("Changed "+username+"'s socket at room "+roomId);
                                        isMember = true;
                                    }
                                }
                            }
//                            else{
//                                System.out.println("room "+roomId+" doesn't exist at server.");
//                            }
//
//                            if(!isMember){
//                                System.out.println(username+" is not a member of room "+roomId);
//                            }

                        }


                        break;

                    case "disconnect":
                        server.removeGuest(this);

                        break;

                    //이 사용자가 새로운 방을 개설하겠다는 메시지
                    //new_room/친구id;친구username;친구2id;친구2username...
                    case "new_room":

                        String friendInfo_string = array[1];

                        System.out.println(username+" created a room.");
                        System.out.println("FriendInfo="+friendInfo_string);

                        //db에 방 정보를 저장하고, 방 id를 얻는다
                        int roomId = connect.insertRoom();
                        String roomName = "Room "+roomId;

                        //방을 개설하고, 현 사용자(=방 개설자)를 그 방에 join 시킨다
                        server.addRoom(roomId, this);

                        //guestList 에서, 나머지 참여자(=초대된 사람들)들의 guest 객체를 찾는다
                        String[] friendInfo_array = friendInfo_string.split(";");
                        System.out.println("friendId_array="+ Arrays.asList(friendInfo_array));


                        //피초대인들의 id와 username을 따로 모아서 arrayList에 저장한다
                        ArrayList<Integer> friendId_list = new ArrayList<>();
//                        ArrayList<String > friendUsername_list = new ArrayList<>();
                        String friendUsername_string = "";

                        int friend_id = 10000;
                        String friend_username = "";

                        for(int i=0; i<friendInfo_array.length; i++){
                            if(i%2==0){
                                friend_id = Integer.valueOf(friendInfo_array[i]);
                                friendId_list.add(friend_id);
                            }else{
//                                friend_username = friendInfo_array[i];
                                friendUsername_string += friendInfo_array[i] + ",";
//                                friendUsername_list.add(friend_username);
                            }

//                            if(i%2==1){
//                                System.out.println("id="+friend_id+" / username="+friend_username);
//                                server.addGuestIdToRoom(roomId, friend_id, friend_username);
//                            }
                        }

                        System.out.println("friend id list="+friendId_list);
                        System.out.println("friend username list="+friendUsername_string);

                        //guestList에서 피초대인들의 guest 객체를 찾는다 -> 이 방에 join 시킨다
                        for(int k=0; k<server.guestList.size(); k++){
                            System.out.println("checking..");

                            MainGuest guest = server.guestList.get(k);

                            for(int j=0; j<friendId_list.size(); j++){
                                System.out.println("guest id="+guest.id+" / friend id="+friendId_list.get(j));


                                if(guest.id == friendId_list.get(j)){
                                    server.addGuestToRoom(roomId, guest);
                                    break;
                                }
                            }
                        }


                        //마지막 "," 제거
                        friendUsername_string = friendUsername_string.substring(0, friendUsername_string.length()-1);


                        //모든 참여자들에게: my_room_created/roomId/방이름/인원/참여자정보(id;username)
                        ArrayList<MainGuest> guestListOfTheRoom = server.roomHashMap.get(roomId);
                        String memberInfo = id+";"+username+";"+friendInfo_string; //방 개설자의 id를 맨 앞에 놓는다
                        server.broadcastToRoom(roomId,"room_created/"+roomId+"/"+roomName+"/"+guestListOfTheRoom.size()+"/"+memberInfo);

                        //모든 참여자에게: 최초의 메시지 발송

//                        msg/roomId/sender_id/sender_username/message
                        //일반 사용자가 보낸 메시지와 구분하기 위해, id와 username을 기억해놔야 한다
                        //id=0, username=server
                        String first_message = "msg/"+roomId+"/0/server/"+username+" invited "+friendUsername_string;
                        server.broadcastToRoom(roomId, first_message);

                        break;

                    // 기존 방에 입장했을 때
                    // return/roomId/roomName
                    case "return":
                        roomId = Integer.valueOf(array[1]);
                        roomName = array[2];

                        server.consoleLog(username+" returned to "+roomName);

                        //방 정보 발송
                        server.broadcastRoomInfoToMyself(this, roomId, roomName);

                        break;

                    //사용자가 메시지를 보냈을 때
                    case "msg":
                        try {
                            int roomId_msg = Integer.valueOf(array[1]);
                            String message = array[2];

                            //내가 보낸 메시지 - 남이 보낸 메시지를 구분하면 안 된다 - 나중에 챗방 목록 업데이트 할때 곤란함
                            //누가 보냈는지 알아야 하므로, 사용자의 id와 닉네임을 붙여서 보낸다
                            server.broadcastToRoom(roomId_msg, "msg/"+roomId_msg+"/"+id+"/"+username+"/"+message);
                        }catch(Exception e){
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            System.out.println(ex);
                        }
                        break;


                    case "closeRoom": //사용자가 방을 닫았을 때(inactive 상태)
                        //네트워크 문제로 소켓이 끊긴 상태일 때도 closeRoom 으로 간주한다

//                        System.out.println(username+" is now inactive at room ("+roomId+")");
//                        server.broadcastToRoomExceptMe(roomId, "inactive/"+username+" is not reading messages.", id);

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
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            System.out.println(ex);


            try {
                //소켓이 끊기면, readline 시 null이 뜬다 -> NullPointerException 발생

                //이 손님을 손님 목록에서 삭제한다
                server.removeGuest(this);



                //-> 해당 사용자를 inactive 상태로 간주한다
//                server.broadcastToRoomExceptMe(roomId, "inactive/"+username+" is not reading messages.", id);

//                server.removeGuestFromRoom(roomId, this);
//                server.removeGuestFromLobby(this);
            } catch (Exception e1) {
                StringWriter sww = new StringWriter();
                e.printStackTrace(new PrintWriter(sww));
                String exx = sww.toString();

                System.out.println(exx);
            }
        }
    }

    //이 사용자에게 메시지 보내기
    public void sendMsg(String msg) throws Exception {

        writer.write(msg + "\n");
        writer.flush();
    }

}

