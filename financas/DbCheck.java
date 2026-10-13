import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mariadb://localhost:3306/financas", "root", "1234");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT email, senha, LENGTH(senha) FROM usuario");
            while (rs.next()) {
                System.out.println("Email: " + rs.getString(1) + " | Senha: " + rs.getString(2) + " | Length: " + rs.getInt(3));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
