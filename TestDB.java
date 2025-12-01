import java.sql.*;

public class TestDB {
    public static void main(String[] args) {
        String dbPath = System.getenv("LOCALAPPDATA") + "\\RegistreComptable\\registre.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("Connexion à: " + dbPath);
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Afficher la structure de la table
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "operations", null);
            
            System.out.println("\n=== COLONNES DE LA TABLE operations ===");
            while (columns.next()) {
                System.out.println(columns.getString("COLUMN_NAME") + " - " + columns.getString("TYPE_NAME"));
            }
            
            // Compter les lignes
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM operations");
            rs.next();
            System.out.println("\n=== NOMBRE D'OPÉRATIONS: " + rs.getInt(1) + " ===");
            
            // Afficher les 3 premières lignes avec toutes les colonnes
            System.out.println("\n=== PREMIÈRES OPÉRATIONS ===");
            ResultSet data = stmt.executeQuery("SELECT * FROM operations LIMIT 3");
            ResultSetMetaData rsmd = data.getMetaData();
            int colCount = rsmd.getColumnCount();
            
            while (data.next()) {
                System.out.println("\n--- Opération ID: " + data.getInt("id") + " ---");
                for (int i = 1; i <= colCount; i++) {
                    String colName = rsmd.getColumnName(i);
                    Object value = data.getObject(i);
                    if (value != null && !value.toString().isEmpty()) {
                        System.out.println("  " + colName + " = " + value);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
