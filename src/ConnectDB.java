import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConnectDB {

    Connection connection = null;
    Statement statement = null;
    ResultSet resultSet = null;

    public void connect() throws Exception{
        try{
            //드라이버 인터페이스를 구현한 클래스를 로딩한다
            Class.forName("com.mysql.jdbc.Driver");

            //드라이버 매니저에게 connection 객체를 달라고 요청한다
            //driver url 뒤에 verify~, useSSl 변수를 넣어주지 않으면 ssl 관련 warning이 뜬다
            String jdbcDriver = "jdbc:mysql://15.164.193.65:3306/app?verifyServerCertificate=false&useSSL=true";
            String dbUser = "root";
            String dbPass = "vhxmvhffldh@2019";
            connection = DriverManager.getConnection(jdbcDriver, dbUser, dbPass); //드라이버 연결
            System.out.println("connected to MYSql");

            //쿼리 실행
//            startQuery();
        }catch(SQLException e){
            System.out.println("jdbc connection error: "+e);
        }

//        finally {
//            if(connection != null){
//                try{
//                    connection.close();
//                }catch (SQLException e){
//                    System.out.println("jdbc connection error: "+e);
//                }
//            }
//        }
    }


    public int insertRoom() throws Exception{

        String sql = "insert into roomInfo(room_name) values(?)";

        int inserted_id;

        //insert 된 id를 반환받기 위해서는, parameter로 Statement.RETURN_GENERATED_KEYS 를 넘겨줘야 한다
        PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pstmt.setString(1,"room");

        int resultCnt = pstmt.executeUpdate(); //영향을 받은 row의 개수를 반환한다
        System.out.println("Succeeded to insert items. row count = "+resultCnt);

        //삽입된 id를 확인한다
        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                inserted_id = generatedKeys.getInt(1);
                System.out.println("-------------------- INSERT -----------------");
                System.out.println("inserted id = "+inserted_id);
                System.out.println("----------------------------------------------");
            }
            else {
                throw new SQLException("Error: No id returned");
            }
        }
        return inserted_id;
    }



    public void startQuery(){
        try{

            //쿼리 수행 시, insert처럼 동적으로 값을 할당해야 할 때 : PrepareStatement() 사용
            //동적 할당이 필요 없으면 : Statement 객체 사용

            //쿼리 결과가 있으면 executeQuery() 메서드를 호출하여 ResultSet 객체에 담는다
            //쿼리 결과가 없으면 executeUpdate() 메서드를 호출하여 int 변수에 결과 값(몇 개의 row가 영향을 받았는지)을 할당한다

            SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");

            Date time = new Date();
            String format_time = format1.format(time);



            //---------------------- insert ----------------------//
            String sql = "insert into roomInfo(room_name, message) values(?,?)";

            //insert 된 id를 반환받기 위해서는, parameter로 Statement.RETURN_GENERATED_KEYS 를 넘겨줘야 한다
            PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1,"room");
            pstmt.setString(2,"jdbc test");

            int resultCnt = pstmt.executeUpdate(); //영향을 받은 row의 개수를 반환한다
            System.out.println("Succeeded to insert items. row count = "+resultCnt);

            //삽입된 id를 확인한다
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int inserted_id = generatedKeys.getInt(1);
                    System.out.println("-------------------- INSERT -----------------");
                    System.out.println("inserted id = "+inserted_id);
                    System.out.println("----------------------------------------------");
                }
                else {
                    throw new SQLException("Error: No id returned");
                }
            }


            //---------------------- select ----------------------//
            statement = connection.createStatement();
            String select_sql = "select * from roomInfo";
//            int insertedCount = statement.executeUpdate("insert salekjset");
            resultSet = statement.executeQuery(select_sql);

            System.out.println("-------------------- SELECT -----------------");
            while(resultSet.next()){
                System.out.println("id:"+resultSet.getInt("id")
                        +" / room_name:"+resultSet.getString("room_name")
                        +" / message:"+resultSet.getString("message"));
            }
            System.out.println("----------------------------------------------");



            //---------------------- update ----------------------//
            String sql_update = "update roomInfo set room_name='updated name2' where id=1";
            pstmt = connection.prepareStatement(sql_update);
            pstmt.executeUpdate();
            System.out.println("-------------------- UPDATE -----------------");
            System.out.println("updated.");
            System.out.println("----------------------------------------------");


            //---------------------- delete ----------------------//
            String sql_delete = "delete from roomInfo where id = 2";
            pstmt = connection.prepareStatement(sql_delete);
            pstmt.executeUpdate();
            System.out.println("-------------------- DELETED -----------------");
            System.out.println("deleted.");
            System.out.println("----------------------------------------------");


        }catch (SQLException e){
            System.out.println("jdbc statement error: "+e);
        }{
            if(resultSet != null){
                try{
                    resultSet.close();
                }catch (SQLException e){
                    System.out.println("jdbc statement error: "+e);
                }
            }

            if(statement != null){
                try{
                    statement.close();
                }catch (SQLException e){
                    System.out.println("jdbc statement error: "+e);
                }
            }
        }

    }


//    public static void main(String args[]) throws Exception {
//
//        ConnectDB con = new ConnectDB();
//
//        con.connect();
//
//
//    }
}
