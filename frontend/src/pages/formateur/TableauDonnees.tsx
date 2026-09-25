import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Space, Spin, Table, Tag, Tooltip, Typography } from "antd";
import { ReloadOutlined } from "@ant-design/icons";
import { api, LigneTableau } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";

const { Text } = Typography;

const INTERVALLE_RAFRAICHISSEMENT_MS = 5000;

interface Props {
  promotionId: number;
}

/**
 * Corrige K48-26 (issue #39) : le tableau se rafraîchit automatiquement (toutes les 5 s) et
 * propose un bouton « Actualiser », pour ne jamais donner l'impression qu'une présence a été
 * perdue alors qu'elle est juste arrivée après le dernier chargement.
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

    const intervalle = setInterval(charger, INTERVALLE_RAFRAICHISSEMENT_MS);
    return () => clearInterval(intervalle);
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
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ReloadOutlined />} onClick={charger} loading={chargement}>
          Actualiser
        </Button>
        <Text type="secondary">Mise à jour automatique toutes les 5 secondes</Text>
      </Space>
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginBottom: 16 }} />}
      {chargement && lignes === null && <Spin />}
      {lignes !== null && (
        <Table rowKey="etudiantId" dataSource={lignes} columns={colonnes} pagination={false} scroll={{ x: true }} />
      )}
    </>
  );
}
