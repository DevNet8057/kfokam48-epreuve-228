import { useEffect, useState } from "react";
import { Alert, Card, Empty, List, Spin, Tag, Typography } from "antd";
import { api, MonExercice } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";
import { useIdentite } from "../../identite/IdentiteContext";

const { Text, Paragraph } = Typography;

const LIBELLES_STATUT: Record<MonExercice["statut"], string> = {
  SANS_RELECTEUR: "En attente d'un relecteur",
  EN_ATTENTE_RELECTURE: "En attente de relecture",
  EN_COURS_RELECTURE: "Relecture en cours",
  RELU: "Relu",
};

/**
 * EF11 : l'étudiant consulte la note et le commentaire reçus, jamais l'identité des relecteurs
 * (RG17). Une seule note rendue sur deux est signalée comme provisoire (RG18 v2).
 */
export function MesExercices() {
  const { identite } = useIdentite();
  const [exercices, setExercices] = useState<MonExercice[] | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);

  useEffect(() => {
    if (!identite?.etudiantId) return;
    let annule = false;
    api
      .listerMesExercices(identite.etudiantId)
      .then((reponse) => {
        if (!annule) setExercices(reponse);
      })
      .catch((cause) => {
        if (!annule) setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de charger vos exercices.");
      });
    return () => {
      annule = true;
    };
  }, [identite?.etudiantId]);

  return (
    <Card style={{ maxWidth: 480, margin: "1rem auto" }} title="Mes exercices">
      {erreur && <Alert type="error" message={erreur} showIcon />}
      {!erreur && exercices === null && <Spin />}
      {exercices !== null && exercices.length === 0 && <Empty description="Aucun exercice déposé." />}
      {exercices !== null && exercices.length > 0 && (
        <List
          dataSource={exercices}
          renderItem={(exercice) => (
            <List.Item>
              <div style={{ width: "100%" }}>
                <Text strong>{exercice.titreSession}</Text>
                <div>
                  {exercice.note === null ? (
                    <Tag>{LIBELLES_STATUT[exercice.statut]}</Tag>
                  ) : (
                    <>
                      <Tag color={exercice.provisoire ? "orange" : "green"}>
                        Note : {exercice.note} / 20{exercice.provisoire ? " (provisoire)" : ""}
                      </Tag>
                    </>
                  )}
                </div>
                {exercice.commentaires.map((commentaire, index) => (
                  <Paragraph key={index} type="secondary" style={{ marginBottom: 0 }}>
                    « {commentaire} »
                  </Paragraph>
                ))}
              </div>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}
