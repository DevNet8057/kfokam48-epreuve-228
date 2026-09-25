import { useEffect, useState } from "react";
import { Alert, Card, Select, Spin, Table, Tag, Tooltip } from "antd";
import { api, LigneTableau, Promotion } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";

/**
 * EF10 : tableau de présence et de notes du formateur. La moyenne vient uniquement de l'API (F3, RG18).
 */
export function TableauFormateur() {
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [promotionId, setPromotionId] = useState<number | null>(null);
  const [lignes, setLignes] = useState<LigneTableau[] | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);

  useEffect(() => {
    api.listerPromotions().then(setPromotions).catch(() => setErreur("Impossible de charger les promotions."));
  }, []);

  useEffect(() => {
    if (promotionId === null) return;
    let annule = false;
    setChargement(true);
    setErreur(null);
    api
      .obtenirTableau(promotionId)
      .then((reponse) => {
        if (!annule) setLignes(reponse);
      })
      .catch((cause) => {
        if (!annule) setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de charger le tableau.");
      })
      .finally(() => {
        if (!annule) setChargement(false);
      });
    return () => {
      annule = true;
    };
  }, [promotionId]);

  const colonnes = [
    { title: "Étudiant", dataIndex: "nom", key: "nom" },
    { title: "Présences", dataIndex: "presences", key: "presences" },
    {
      title: "Détail présences",
      key: "detailPresences",
      render: (_: unknown, ligne: LigneTableau) => (
        <>
          {ligne.detailPresences.map((detail) => (
            <Tooltip key={detail.sessionId} title={detail.titre}>
              <Tag color={detail.present ? "green" : "default"}>
                {detail.present ? (detail.source === "FORMATEUR" ? "Ajoutée par le formateur" : "Présent") : "Absent"}
              </Tag>
            </Tooltip>
          ))}
        </>
      ),
    },
    { title: "Exercices déposés", dataIndex: "exercicesDeposes", key: "exercicesDeposes" },
    { title: "Exercices en attente", dataIndex: "exercicesEnAttente", key: "exercicesEnAttente" },
    {
      title: "Moyenne",
      dataIndex: "moyenne",
      key: "moyenne",
      render: (moyenne: number | null) => (moyenne === null ? "—" : moyenne),
    },
    { title: "Relectures en attente", dataIndex: "relecturesEnAttente", key: "relecturesEnAttente" },
  ];

  return (
    <Card style={{ maxWidth: 900, margin: "1rem auto" }} title="Tableau de la promotion">
      <Select
        style={{ width: "100%", marginBottom: 16 }}
        placeholder="Choisir une promotion"
        options={promotions.map((p) => ({ value: p.id, label: p.nom }))}
        onChange={(valeur) => setPromotionId(valeur)}
      />
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginBottom: 16 }} />}
      {chargement && <Spin />}
      {!chargement && lignes !== null && (
        <Table
          rowKey="etudiantId"
          dataSource={lignes}
          columns={colonnes}
          pagination={false}
          scroll={{ x: true }}
        />
      )}
    </Card>
  );
}
