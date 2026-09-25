package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Changement de besoin (Enveloppe, étape 3, C2) : chaque exercice est désormais relu par DEUX
 * pairs distincts. La contrainte UNIQUE(exercice_id) posée par V1 empêchait une deuxième ligne de
 * relecture pour le même exercice ; on la remplace par UNIQUE(exercice_id, relecteur_id).
 *
 * Migration Java plutôt que SQL : le nom généré automatiquement pour cette contrainte diffère
 * entre PostgreSQL ("relecture_exercice_id_key") et H2 en mode PostgreSQL (utilisé par les tests,
 * ENF8) — on le retrouve via information_schema, portable sur les deux moteurs.
 * Base déjà remplie : ALTER TABLE conserve les lignes existantes, aucune donnée perdue.
 */
public class V3__DeuxRelecteurs extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String nomContrainte = trouverNomContrainteUniqueExerciceId(connection);
        try (Statement statement = connection.createStatement()) {
            // Guillemets : H2 (DATABASE_TO_LOWER=TRUE) stocke le nom en minuscules, un
            // identifiant non guillemeté serait replié en majuscules et ne correspondrait plus.
            statement.execute("ALTER TABLE relecture DROP CONSTRAINT \"" + nomContrainte + "\"");
            statement.execute(
                    "ALTER TABLE relecture ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id)");
        }
    }

    private String trouverNomContrainteUniqueExerciceId(Connection connection) throws Exception {
        String requete = """
                select tc.constraint_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                where tc.table_name = 'relecture'
                  and tc.constraint_type = 'UNIQUE'
                  and kcu.column_name = 'exercice_id'
                """;
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(requete)) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        throw new IllegalStateException("Contrainte UNIQUE sur relecture.exercice_id introuvable.");
    }
}
