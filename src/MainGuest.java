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

                    //피카츄 사진을 분석하라는 메시지가 왔을 때
                    case "pikachu":
                        String image_name = array[1];
                        server.send_message_to_tensorflow_server(image_name);

                        break;

                    //피카츄 이미지분석 서버로부터의 메시지
                    //pikachu_server/성공여부/원본파일이름
                    case "pikachu_server":

                        String success_or_fail = array[1];
                        String filename_origin = array[2];

                        String[] filename_split = filename_origin.split("_");
                        int user_id = Integer.valueOf(filename_split[0]);

                        if(success_or_fail.equals("success")){

                            String output_filename = "processed_"+filename_origin;

                            server.send_message_to_guest(user_id, "success/"+output_filename);
                        }else{
                            server.send_message_to_guest(user_id, "fail/"+filename_origin);
                        }

                        break;

                    //사용자가 접속하였다는 메시지
                    case "connect":

                        id = Integer.valueOf(array[1]); //사용자 id
                        username = array[2]; //사용자 username
                        String myRoom_id = array[3]; //사용자가 가입한 방id 목록

                        System.out.println("id="+id+" / username="+username);

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
                        int numberOfInvitedMembers;

                        System.out.println(username+" created a room.");
                        System.out.println("FriendInfo="+friendInfo_string);

                        //db에 방 정보를 저장하고, 방 id를 얻는다
                        int roomId = connect.insertRoom();

                        //방을 개설하고, 현 사용자(=방 개설자)를 그 방에 join 시킨다
                        server.addRoom(roomId, this);

                        //guestList 에서, 나머지 참여자(=초대된 사람들)들의 guest 객체를 찾는다
                        String[] friendInfo_array = friendInfo_string.split(";");
                        System.out.println("friendId_array="+ Arrays.asList(friendInfo_array));


                        String roomName = "";
                        numberOfInvitedMembers = friendInfo_array.length/2; //초대한 친구의 수
                        if(numberOfInvitedMembers == 1){ //1대1 채팅일 경우
                            roomName = "personal_chat";
                        }else{//그룹 채팅일 경우
                            roomName = "Group chat";
                        }


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

                        //1대1채팅의 경우, '방 이름 = 상대방 이름' 으로 설정한다
                        if(numberOfInvitedMembers == 1){//1대1 채팅일 경우

                            //이 사용자에게 보내는 메시지
                            roomName = friendInfo_array[1]; //방이름 = 상대방의 이름
                            sendMsg("room_created/"+roomId+"/"+roomName+"/"+guestListOfTheRoom.size()+"/"+memberInfo);

                            //상대방에게 보내는 메시지
                            ArrayList<MainGuest> guestsOfTheRoom = server.roomHashMap.get(roomId);
                            for (MainGuest guest : guestsOfTheRoom) {
                                if(guest.id != id){
                                    roomName = username; //방이름 = 이 사용자의 이름
                                    guest.sendMsg("room_created/"+roomId+"/"+roomName+"/"+guestListOfTheRoom.size()+"/"+memberInfo);
                                }
                            }
                        }else{ //그룹 채팅일 경우
                            server.broadcastToRoom(roomId,"room_created/"+roomId+"/"+roomName+"/"+guestListOfTheRoom.size()+"/"+memberInfo);
                        }

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
                        //msg/roomId/내용
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

                    case "msg_image":
                        //msg_image/roomId/파일이름1;파일이름2;파일이름3..
                        try {
                            int roomId_img = Integer.valueOf(array[1]);
                            String filename_string = array[2];

                            System.out.println("msg_image");

                            //이 사용자를 제외한 채팅방 참여자에게 메시지를 전달한다
                            //누가 보냈는지 알아야 하므로, 사용자의 id와 닉네임을 붙여서 보낸다
                            //@@@메시지 내용에 image!-!를 붙인다. 이미지라는 것을 표시하기 위함
                            String message = "msg/"+roomId_img+"/"+id+"/"+username+"/image!-!"+filename_string;
                            System.out.println("msg="+message);

                            server.broadcastToRoomExceptMe(roomId_img, message, id);

                        }catch(Exception e){
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            System.out.println(ex);
                        }
                        break;

                    case "msg_video":
                        //msg_video/room_id/썸네일 파일 이름/동영상 파일 이름
                        try {
                            int roomId_video = Integer.valueOf(array[1]);
                            String thumbnail_filename = array[2];
                            String video_filename = array[3];

                            System.out.println("msg_video");

                            //이 사용자를 제외한 채팅방 참여자에게 메시지를 전달한다
                            //누가 보냈는지 알아야 하므로, 사용자의 id와 닉네임을 붙여서 보낸다
                            //@@@메시지 내용에 video!-!를 붙인다. 비디오라는 것을 표시하기 위함
                            String message = "msg/"+roomId_video+"/"+id+"/"+username+"/video!-!"+thumbnail_filename+"!-!"+video_filename;
                            System.out.println("msg="+message);

                            server.broadcastToRoomExceptMe(roomId_video, message, id);

                        }catch(Exception e){
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            System.out.println(ex);
                        }
                        break;

                    case "invite": //기존 채팅방에 누군가를 초대했을 때

                        int roomId_invitation = Integer.valueOf(array[1]);
                        String invited_friendInfo_string = array[2];

                        //guestList 에서, 나머지 참여자(=초대된 사람들)들의 guest 객체를 찾는다
                        String[] invited_friendInfo_array = invited_friendInfo_string.split(";");
                        int numberOfNewMembers_invitation = invited_friendInfo_array.length/2;

                        System.out.println("invited_friendInfo_array="+ Arrays.asList(invited_friendInfo_array));


                        //피초대인들의 id와 username을 따로 모아서 arrayList에 저장한다
                        ArrayList<Integer> invited_friendId_list = new ArrayList<>();
                        ArrayList<String > invited_friendUsername_list = new ArrayList<>();
                        String invited_friendUsername_string = "";

                        int invited_friend_id = 10000;
                        String invited_friend_username = "";

                        for(int i=0; i<invited_friendInfo_array.length; i++){
                            if(i%2==0){
                                friend_id = Integer.valueOf(invited_friendInfo_array[i]);
                                invited_friendId_list.add(friend_id);
                            }else{
                                invited_friend_username = invited_friendInfo_array[i];
                                invited_friendUsername_list.add(invited_friend_username);

                                invited_friendUsername_string += invited_friend_username + ",";
                            }

                        }

                        //마지막 "," 제거
                        invited_friendUsername_string = invited_friendUsername_string.substring(0, invited_friendUsername_string.length()-1);


                        //기존 참여자들에게 알림 발송
                        //new_member/방id/추가memberInfo(id;username)
                        String message_newMember = "new_member/"+roomId_invitation+"/"+invited_friendInfo_string;
                        server.broadcastToRoom(roomId_invitation, message_newMember);


                        //이 방의 전체 멤버info를 구한다(추가된 멤버info + 기존 멤버info)
                        String all_memberInfo = invited_friendInfo_string;
                        ArrayList<MainGuest> guestList_room = server.roomHashMap.get(roomId_invitation);
                        for(int i=0; i<guestList_room.size(); i++){
                            MainGuest guest = guestList_room.get(i);
                            all_memberInfo += ";"+ guest.id + ";" + guest.username;
                        }
                        System.out.println("all_memberInfo="+all_memberInfo);


                        //방 이름을 정한다
                        String roomName_invitation = "";
                        int numberOfMembers_origin = guestList_room.size();
                        if(numberOfMembers_origin == 1){

                            if(numberOfNewMembers_invitation == 1){
                                //1명있는 방에 혼자 초대된 것이라면 -> 1대1 채팅 -> roomName = 상대방 이름
                                roomName_invitation = all_memberInfo.split(";")[3];

                            }else if(numberOfNewMembers_invitation > 1){
                                //1명있는 방에 2명이상 초대된 것이면 -> roomName = group chat
                                roomName_invitation = "Group chat";
                            }

                        }else if(numberOfMembers_origin > 1){
                            //2명이상 있는 방에 초대된 것이라면 -> roomName = group chat
                            roomName_invitation = "Group chat";
                        }




                        //guestList에서 피초대인들의 guest 객체를 찾는다
                        // -> 이 방에 join 시키고 + 초대 되었다고 알림 발송
                        for(int k=0; k<server.guestList.size(); k++){
                            System.out.println("checking..");

                            MainGuest guest = server.guestList.get(k);

                            for(int j=0; j<invited_friendId_list.size(); j++){
                                System.out.println("guest id="+guest.id+" / friend id="+invited_friendId_list.get(j));

                                if(guest.id == invited_friendId_list.get(j)){

                                    //이 방에 join 시킨다
                                    server.addGuestToRoom(roomId_invitation, guest);

                                    //초대 알림 발송
                                    //invited/방id/방이름/memberInfo
                                    String message_invitation = "invited/"+roomId_invitation+"/"+roomName_invitation+"/"+all_memberInfo;
                                    guest.sendMsg(message_invitation);
                                    break;
                                }
                            }
                        }


                        //서버 메시지를 발송한다
                        //msg/방id/0/server/ㅇㅇ invited xx, aa, ee
                        String serverMsg_invitation = "msg/"+roomId_invitation+"/0/server/"+username+" invited "+invited_friendUsername_string;
                        server.broadcastToRoom(roomId_invitation, serverMsg_invitation);

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
//                this.interrupt(); 이거 해야되나??


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

