import { useEffect, useState } from "react";
import { Alert, Button, Card, Empty, Input, List, Space, Spin, Tag, Typography } from "antd";
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

/** EF12 / RG16 : remplaçable tant qu'aucune relecture n'a commencé (donc pas EN_COURS_RELECTURE ni RELU). */
const REMPLACABLE: MonExercice["statut"][] = ["SANS_RELECTEUR", "EN_ATTENTE_RELECTURE"];

/**
 * EF11 : l'étudiant consulte la note et le commentaire reçus, jamais l'identité des relecteurs
 * (RG17). Une seule note rendue sur deux est signalée comme provisoire (RG18 v2).
 * EF12 : remplacer le lien tant qu'aucune relecture n'a commencé (RG16).
 */
export function MesExercices() {
  const { identite } = useIdentite();
  const [exercices, setExercices] = useState<MonExercice[] | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [enEdition, setEnEdition] = useState<number | null>(null);
  const [nouveauLien, setNouveauLien] = useState("");

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

  async function remplacerLien(exercice: MonExercice) {
    setErreur(null);
    try {
      await api.remplacerLien(exercice.id, nouveauLien);
      setExercices((actuels) =>
        (actuels ?? []).map((e) => (e.id === exercice.id ? { ...e, lien: nouveauLien } : e))
      );
      setEnEdition(null);
      setNouveauLien("");
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de remplacer le lien.");
    }
  }

  return (
    <Card style={{ maxWidth: 480, margin: "1rem auto" }} title="Mes exercices">
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginBottom: 16 }} />}
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
                    <Tag color={exercice.provisoire ? "orange" : "green"}>
                      Note : {exercice.note} / 20{exercice.provisoire ? " (provisoire)" : ""}
                    </Tag>
                  )}
                </div>
                {exercice.commentaires.map((commentaire, index) => (
                  <Paragraph key={index} type="secondary" style={{ marginBottom: 0 }}>
                    « {commentaire} »
                  </Paragraph>
                ))}
                {REMPLACABLE.includes(exercice.statut) && (
                  <div style={{ marginTop: 8 }}>
                    {enEdition === exercice.id ? (
                      <Space.Compact style={{ width: "100%" }}>
                        <Input
                          value={nouveauLien}
                          onChange={(e) => setNouveauLien(e.target.value)}
                          placeholder="https://…"
                        />
                        <Button type="primary" onClick={() => remplacerLien(exercice)}>
                          Valider
                        </Button>
                        <Button onClick={() => setEnEdition(null)}>Annuler</Button>
                      </Space.Compact>
                    ) : (
                      <Button
                        size="small"
                        onClick={() => {
                          setEnEdition(exercice.id);
                          setNouveauLien(exercice.lien);
                        }}
                      >
                        Remplacer le lien
                      </Button>
                    )}
                  </div>
                )}
              </div>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}
