import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.ServerSocket;

import java.net.Socket;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.Set;


//채팅방을 관리하는 서버측 클래스
public class MainServer {

    public static final int ServerPort = 8000;

    ArrayList<MainGuest> guestList; //손님 목록

    HashMap<Integer, ArrayList<MainGuest>> roomHashMap; //방 목록 맵(방id, 방에 속한 클라이언트 목록)

//    HashMap<Integer, HashMap<Integer, String>> roomHashMap_onlyId; //방 목록 맵(방id, 방에 속한 클라이언트 정보<id, username>)



    void initNet() throws Exception {

        roomHashMap = new HashMap<Integer, ArrayList<MainGuest>>();
//        roomHashMap_onlyId = new HashMap<>();

        guestList = new ArrayList<MainGuest>();

        ConnectDB connectDB = new ConnectDB();
        connectDB.connect(); //db 연결

        //서버소켓 생성. 클라이언트 접속을 기다림
        ServerSocket serverSocket = new ServerSocket(ServerPort);
        consoleLog("Listening to 8000..");

        while (true) {

            Socket socket = serverSocket.accept();

            consoleLog(socket.getInetAddress()+"is connected");

            //MainGuest 클래스: 클라이언트와의 데이터 송수신을 처리하는 서버측 클래스(보기 편하라고 나눔)
            //생성자 1 (Server): 서버의 메소드를 사용하기 위해 필요
            //생성자 2 (socket): 클라이언트와의 데이터 송수신을 위해 필요
            MainGuest guestThread = new MainGuest(this, socket, connectDB);

            guestThread.start();
            addGuest(guestThread); //새로운 게스트 객체를 로비 사용자 리스트에 추가한다
        }

    }

    ///////////////////////////////////////

    void enterExistingRoom(int roomId, String roomName, MainGuest guest) throws Exception{

        boolean isAlreadyMember = false;

        //기존에 저장되어있던 socket을 현재 것으로 바꾼다
        ArrayList<MainGuest> listOfMainGuestsInTheRoom = roomHashMap.get(roomId);
        for(int i=0; i<listOfMainGuestsInTheRoom.size(); i++){
            MainGuest anonymous_guest = listOfMainGuestsInTheRoom.get(i);
            if(anonymous_guest.id == guest.id){

                anonymous_guest.socket = guest.socket;
                anonymous_guest.reader = guest.reader;
                anonymous_guest.writer = guest.writer;

                consoleLog(guest.username +" is already a member of "+roomName);
                isAlreadyMember = true;
            }
        }

        if(isAlreadyMember){
            broadcastToRoomExceptMe(roomId, "return/"+guest.username+" is back.", guest.id); //다른 사람에게 보내는 메시지
            guest.sendMsg("myReturn/Welcome back.");//나에게 보내는 메시지
//            broadcastRoomInfo(roomId, roomName);
        }else{
            consoleLog("Error) "+guest.username+ " is a member of "+roomName+ ". But not listed!!");
        }

    }



    //손님 목록에 해당 사용자 객체를 추가한다
    void addGuest(MainGuest guest) {
        //있는지 확인하고, 있으면 -> socket 바꿔치기
        //없으면 -> 새로 추가

        guestList.add(guest);
        consoleLog("Number of Guests : " + guestList.size());

    }


    //손님의 연결이 끊겼을 경우 -> 손님 목록에서 이 사람을 삭제
    void removeGuest(MainGuest guest){

        int number_of_guests_prev = guestList.size();
        String guest_name = guest.username;

        guestList.remove(guest);
        consoleLog(guest_name+" is removed from guestList. Number of Guests : "+number_of_guests_prev+" -> "+ guestList.size());

    }



    //방목록에서 해당 방을 제거
    void removeRoom(int roomId){

        if(roomHashMap.get(roomId).size()==0){ //방에 아무도 없는 것을 확인하고

            //해당 방을 방 목록(hashMap)에서 삭제한다
            roomHashMap.remove(roomId);
        }

    }


    //사용자가 방에서 나갔을 때, 후처리하는 메소드
    void removeMainGuestFromRoom(int roomId, MainGuest guest) throws Exception{

        //방 이름(key)으로 guest List를 불러온다. 이 list에서 해당 guest를 삭제한다
        //value 로 삭제가 되나?? 된다. object를 삭제할 경우, remove(object) 하면 해당 object가 삭제된다
        //이 경우 MainGuest 객체를 목록에서 삭제하는 것이다
        //integer 를 삭제할 경우, remove((Integer) number) 라고 해야함
        roomHashMap.get(roomId).remove(guest);
        consoleLog(guest.username+" is removed from the room");

        //해당 사용자가 나갔다고 방에 방송한다
        //이미 목록에서 해당 사용자를 삭제했기 때문에, broacastToRoomExceptMe()를 사용하지 않아도 된다
        broadcastToRoom(roomId, "out/"+guest.id+"/"+guest.username);
    }

    // /////////////////////////////////////////////


    //사용자를 방의 손님목록에서 제거하는 메소드
    int removeGuestFromRoom(int roomId, MainGuest guest) {

        //방 목록 맵에 저장되어 있던, 이 방의 손님 리스트를 참조한다
        ArrayList<MainGuest> guestList_room = roomHashMap.get(roomId);

        //원래 손님 수
        int number_of_guests_before = guestList_room.size();

        //이 손님을 리스트에서 제거한다
        guestList_room.remove(guest);

        int number_of_guests_after = guestList_room.size();

        //방 이름과 손님 수를 서버 콘솔에 출력한다
        consoleLog("Removed "+guest.username+" from room " + roomId + ", " +
                "Number of Guests : " + number_of_guests_before +" -> " + number_of_guests_after);

        //이 방에 아무도 남지 않았으면 -> roomHashMap 에서 이 방을 삭제한다
        if(number_of_guests_after == 0){

            String rooms_before = roomHashMap.keySet().toString();

            roomHashMap.remove(roomId);
            consoleLog("This room is empty. Removed from the roomHashMap");

            String rooms_after = roomHashMap.keySet().toString();

            consoleLog("Room list: "+rooms_before+" -> "+rooms_after);
        }

        return number_of_guests_after;
    }



    //사용자를 방의 손님목록에 추가하는 메소드
    void addGuestToRoom(int roomId, MainGuest guest) {

        //방 목록 맵에 저장되어 있던, 이 방의 손님 리스트를 참조한다
        ArrayList<MainGuest> guestList_room = roomHashMap.get(roomId);

        //그 리스트에 손님을 추가한다
        guestList_room.add(guest);

        //방 이름과 손님 수를 서버 콘솔에 출력한다
        consoleLog("roomId : " + roomId + ", Number of Guests : " + guestList_room.size());
    }


    //사용자를 방의 손님목록에 추가하는 메소드
//    void addGuestIdToRoom(int roomId, int guest_id, String username) {
//
//        //방 목록 맵에 저장되어 있던, 이 방의 손님정보 hashmap<user_id, username>을 참조한다
//        HashMap<Integer, String> guestInfoInTheRoom_copied = roomHashMap_onlyId.get(roomId);
//
//        //그 리스트에 손님을 추가한다
//        guestInfoInTheRoom_copied.put(guest_id, username);
//
//        //방 이름과 손님 수를 서버 콘솔에 출력한다
//        consoleLog(guest_id+" is added to id_only list. total members of the room="+guestInfoInTheRoom_copied.size());
//    }



    //새로운 방을 생성하는 메소드
    void addRoom(int roomId, MainGuest guest) {

        //새로운 손님 리스트를 생성
        ArrayList<MainGuest> listOfMainGuestsOfNewRoom = new ArrayList<MainGuest>(); //손님 객체를 저장

        //이 리스트에 방을 만든 게스트를 추가한다
        listOfMainGuestsOfNewRoom.add(guest);

        //방 목록 hashmap에 이 방을 추가한다
        roomHashMap.put(roomId, listOfMainGuestsOfNewRoom);

        consoleLog("A room is created: " + roomId);
        consoleLog("total members of the room(it must be 1; guest object)" + listOfMainGuestsOfNewRoom.size());
    }


    //각 방의 인원을 반환하는 메소드
    String getNumberOfMainGuestsInEachRoom(Set<Integer> roomList) throws Exception{

        StringBuffer buffer_numberOfMainGuestsInEachRoom = new StringBuffer("roomnum/"); //방에 사람수

        //각 방의 인원을 한 문장으로 나타냄
        for(int roomId : roomList){
            buffer_numberOfMainGuestsInEachRoom.append(roomHashMap.get(roomId).size()+"/");
        }

        return buffer_numberOfMainGuestsInEachRoom.toString();
    }

    /////////////////////////////////////////////////////////////


    //특정 사용자를 대기실 손님 목록에서 삭제한다
//    void removeMainGuestFromLobby(MainGuest guest) {
//
//        int number_before = guestList.size();
//        String guest_id = guest.id;
//        String guest_username = guest.username;
//
//        guestList.remove(guest);
//
//        int number_after = guestList.size();
//        consoleLog("Has removed guest "+guest_id+"/"+guest_username+". Number of Entire MainGuests : "+number_before+"->"+number_after);
//    }


    //대기실 접속자에게 메시지를 보내는 메소드
//    void broadcastToEveryoneAtLobby(String msg) throws Exception {
//
//        try {
//            for (MainGuest guest : guestList) {
//                guest.sendMsg(msg);
//            }
//
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//
//    }



    //특정 방에 있는 사람들에게 메시지를 보낸다
    void broadcastToRoom(int roomId, String msg) throws Exception {

        ArrayList<MainGuest> guestListOfTheRoom = roomHashMap.get(roomId);

        for (MainGuest guest : guestListOfTheRoom) {
            guest.sendMsg(msg);
        }
    }


    //나를 제외한 방 사람들에게 메시지를 보낸다
    void broadcastToRoomExceptMe(int roomId, String msg, int userId) throws Exception {

        consoleLog("broadcastToRoomExceptMe start");

        ArrayList<MainGuest> guestListOfTheRoom = roomHashMap.get(roomId);
        consoleLog("guestList of room "+roomId+" : "+guestListOfTheRoom);

        for (MainGuest guest : guestListOfTheRoom) {
            if(guest.id != userId){
                consoleLog("sending msg to "+guest.username);
                guest.sendMsg(msg);
            }
        }

        consoleLog("broadcastToRoomExceptMe finish");
    }


    //대기실에 있는 모든 손님의 이름을 broadcast 한다
    //guestlist/홍길동/김길동/이길동/ --이런식으로 클라이언트에 보내면,
    //클라이언트가 '/' 단위로 쪼개서 화면에 출력할 것임
//    void broadcastMainGuestlistAtLobby() throws Exception {
//
//        StringBuffer buffer = new StringBuffer("guestlist/"); //클라이언트에 보내는 신호
//
//        for (MainGuest guest : guestList) {
//            buffer.append(guest.id + "/");
//        }
//
//        broadcastToEveryoneAtLobby(buffer.toString());
//    }




    //텐서플로우 서버에 메시지를 보낸다
    void send_message_to_tensorflow_server(String filename) throws Exception {

        consoleLog("send message to tensorflow server");

        for (MainGuest guest : guestList) {
            if(guest.username.equals("pikachu")){
                consoleLog("found pikachu guest. sending message..");
                guest.sendMsg("start/"+filename);
            }
        }

    }


    //특정인에게 메시지를 보낸다
    void send_message_to_guest(int user_id, String message) throws Exception {

        consoleLog("send message to guest "+user_id);

        for (MainGuest guest : guestList) {
            if(guest.id == user_id){
                consoleLog("found guest. sending message..");
                guest.sendMsg("pikachu_output/"+message);
            }
        }

    }





    //특정 방의 정보를 발송한다
//    roomInfo/방이름/참여자 수/user1, user2, user3 -- 이런 식으로
    void broadcastRoomInfo(int roomId, String roomName) throws Exception {

        ArrayList<MainGuest> guestListOfTheRoom = roomHashMap.get(roomId);

        StringBuffer buffer_roomInfo = new StringBuffer("roomInfo/"+roomName+" ("+guestListOfTheRoom.size()+") ");

        for (MainGuest guest : guestListOfTheRoom) {
            buffer_roomInfo.append(guest.username + ", ");
        }

        broadcastToRoom(roomId, buffer_roomInfo.toString());
    }


    //나에게만 방 정보를 발송한다
    void broadcastRoomInfoToMyself(MainGuest guest, int roomId, String roomName) throws Exception {

        ArrayList<MainGuest> guestListOfTheRoom = roomHashMap.get(roomId);

        int numberOfMembers = guestListOfTheRoom.size();
        String roomInfo = "";

        if(numberOfMembers <=2){
            roomInfo = roomName;
        }else{
            roomInfo = roomName+" ("+numberOfMembers+")";
        }

        String memberInfo = "";

        //모든 참여자의
        for (MainGuest member : guestListOfTheRoom) {
            memberInfo += member.id+";"+member.username +";";
        }

        //마지막 ';' 제거
        memberInfo = memberInfo.substring(0, memberInfo.length()-1);

        guest.sendMsg("roomInfo/"+roomInfo+"/"+memberInfo);
    }


    //특정 방의 정보를 발송한다
    //roomInfo/방이름/참여자 수/user1, user2, user3 -- 이런 식으로
//    void broadcastRoomInfo(int roomId, String roomName) throws Exception {
//
//        HashMap<Integer, String> guestListOfTheRoom = roomHashMap_onlyId.get(roomId);
//
//        StringBuffer buffer_roomInfo = new StringBuffer("roomInfo/"+roomName+" ("+guestListOfTheRoom.size()+") ");
//
//        //key=user id / .get(key)=username
//        for (int key : guestListOfTheRoom.keySet()) {
//            buffer_roomInfo.append(guestListOfTheRoom.get(key)+", ");
//        }
//
//        //마지막 쉼표 제거
//        buffer_roomInfo.deleteCharAt(buffer_roomInfo.length()-1);
//
//        broadcastToRoom(roomId, buffer_roomInfo.toString());
//    }



    //서버 콘솔에 로그 남기기
    public void consoleLog(String message){
        System.out.println(message);
    }




    public static void main(String args[]) throws Exception {

        MainServer server = new MainServer();

        server.initNet();

    }

}
