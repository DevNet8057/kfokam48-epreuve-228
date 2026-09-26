package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * K48-29 : complète V3 (deux relecteurs par exercice) sans la modifier.
 *
 * Sous H2 (base des tests), la suppression de la contrainte UNIQUE(exercice_id) par V3 laisse en
 * place l'index unique qui la portait, car la clé étrangère relecture.exercice_id le réutilise :
 * un deuxième relecteur restait refusé. Sous PostgreSQL, cet index a bien disparu avec V3.
 *
 * On cherche donc, via l'API JDBC standard (portable), un index unique portant sur la seule colonne
 * exercice_id. S'il n'y en a pas (PostgreSQL), rien n'est modifié. Sinon on supprime la clé
 * étrangère puis l'index, et on recrée la clé étrangère (non unique). La contrainte
 * UNIQUE(exercice_id, relecteur_id) de V3 reste en place. Aucune donnée n'est touchée.
 */
public class V4__IndexRelectureNonUnique extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DatabaseMetaData meta = connection.getMetaData();
        String schema = connection.getSchema();

        List<String> indexesUniquesSurExerciceSeul = new ArrayList<>();
        Map<String, List<String>> colonnesParIndex = new LinkedHashMap<>();
        try (ResultSet index = meta.getIndexInfo(null, schema, "relecture", true, false)) {
            while (index.next()) {
                String nom = index.getString("INDEX_NAME");
                String colonne = index.getString("COLUMN_NAME");
                if (nom != null && colonne != null) {
                    colonnesParIndex.computeIfAbsent(nom, cle -> new ArrayList<>()).add(colonne.toLowerCase());
                }
            }
        }
        colonnesParIndex.forEach((nom, colonnes) -> {
            if (colonnes.equals(List.of("exercice_id"))) {
                indexesUniquesSurExerciceSeul.add(nom);
            }
        });
        if (indexesUniquesSurExerciceSeul.isEmpty()) {
            return;
        }

        String cleEtrangere = null;
        try (ResultSet cles = meta.getImportedKeys(null, schema, "relecture")) {
            while (cles.next()) {
                if ("exercice_id".equalsIgnoreCase(cles.getString("FKCOLUMN_NAME"))) {
                    cleEtrangere = cles.getString("FK_NAME");
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            if (cleEtrangere != null) {
                statement.execute("ALTER TABLE relecture DROP CONSTRAINT \"" + cleEtrangere + "\"");
            }
            for (String index : indexesUniquesSurExerciceSeul) {
                statement.execute("DROP INDEX IF EXISTS \"" + index + "\"");
            }
            statement.execute(
                    "ALTER TABLE relecture ADD CONSTRAINT fk_relecture_exercice FOREIGN KEY (exercice_id) REFERENCES exercice(id)");
        }
    }
}
