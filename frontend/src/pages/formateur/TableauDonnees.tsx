import { useCallback, useEffect, useState } from "react";
import { Alert, Spin, Table, Tag, Tooltip } from "antd";
import { api, LigneTableau } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";

interface Props {
  promotionId: number;
}

/**
 * BUG K48-26 (issue #39) : un seul chargement au montage, jamais de rafraîchissement ensuite.
 * Une présence enregistrée après ce chargement semble "perdue" jusqu'à un remontage du composant.
 */
export function TableauDonnees({ promotionId }: Props) {
  const [lignes, setLignes] = useState<LigneTableau[] | null>(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(async () => {
    setErreur(null);
    try {
      setLignes(await api.obtenirTableau(promotionId));
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de charger le tableau.");
    }
  }, [promotionId]);

  useEffect(() => {
    setChargement(true);
    charger().finally(() => setChargement(false));
  }, [charger]);

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
    <>
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginBottom: 16 }} />}
      {chargement && lignes === null && <Spin />}
      {lignes !== null && (
        <Table rowKey="etudiantId" dataSource={lignes} columns={colonnes} pagination={false} scroll={{ x: true }} />
      )}
    </>
  );
}
