import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Card, Popconfirm, Select, Space, Spin, Table, Tag } from "antd";
import { api, Etudiant, Promotion, SessionResume } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";

/**
 * Sessions d'une promotion côté formateur. EF5 : ajouter une présence à la main (RG8), marquée
 * « ajoutée par le formateur » dans le tableau (Q14). EF13 : clôturer une session (irréversible, RG20).
 */
export function GestionSessions() {
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [promotionId, setPromotionId] = useState<number | null>(null);
  const [sessions, setSessions] = useState<SessionResume[] | null>(null);
  const [etudiants, setEtudiants] = useState<Etudiant[]>([]);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [succes, setSucces] = useState<string | null>(null);

  useEffect(() => {
    api.listerPromotions().then(setPromotions).catch(() => setErreur("Impossible de charger les promotions."));
  }, []);

  const chargerSessions = useCallback(async (id: number) => {
    setChargement(true);
    setErreur(null);
    try {
      const [listeSessions, listeEtudiants] = await Promise.all([
        api.listerSessions(id),
        api.listerEtudiantsDeLaPromotion(id),
      ]);
      setSessions(listeSessions);
      setEtudiants(listeEtudiants);
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de charger les sessions.");
    } finally {
      setChargement(false);
    }
  }, []);

  useEffect(() => {
    if (promotionId !== null) chargerSessions(promotionId);
  }, [promotionId, chargerSessions]);

  async function ajouterPresence() {
    if (sessionId === null || etudiantId === null) return;
    setErreur(null);
    setSucces(null);
    try {
      await api.ajouterPresenceManuelle(sessionId, etudiantId);
      const nom = etudiants.find((e) => e.id === etudiantId)?.nom ?? "L'étudiant";
      setSucces(`${nom} est marqué présent (ajouté par le formateur).`);
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Impossible d'ajouter la présence.");
    }
  }

  async function cloturer(session: SessionResume) {
    setErreur(null);
    setSucces(null);
    try {
      await api.cloturerSession(session.id);
      setSucces(`La session « ${session.titre} » est clôturée.`);
      if (promotionId !== null) await chargerSessions(promotionId);
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de clôturer la session.");
    }
  }

  const colonnes = [
    { title: "Session", dataIndex: "titre", key: "titre" },
    { title: "Code", dataIndex: "code", key: "code" },
    {
      title: "Expiration du code",
      dataIndex: "expirationAt",
      key: "expirationAt",
      render: (valeur: string) => new Date(valeur).toLocaleString("fr-FR"),
    },
    {
      title: "État",
      key: "etat",
      render: (_: unknown, session: SessionResume) =>
        session.clotureAt ? <Tag>Clôturée</Tag> : <Tag color="green">Ouverte</Tag>,
    },
    {
      title: "Action",
      key: "action",
      render: (_: unknown, session: SessionResume) =>
        session.clotureAt ? null : (
          <Popconfirm
            title="Clôturer cette session ?"
            description="Irréversible : plus de présence, de dépôt ni de relecture."
            okText="Clôturer"
            cancelText="Annuler"
            onConfirm={() => cloturer(session)}
          >
            <Button danger size="small">
              Clôturer
            </Button>
          </Popconfirm>
        ),
    },
  ];

  const sessionsOuvertes = (sessions ?? []).filter((session) => !session.clotureAt);

  return (
    <Card style={{ maxWidth: 900, margin: "1rem auto" }} title="Sessions de la promotion">
      <Select
        style={{ width: "100%", marginBottom: 16 }}
        placeholder="Choisir une promotion"
        options={promotions.map((p) => ({ value: p.id, label: p.nom }))}
        onChange={(valeur) => setPromotionId(valeur)}
      />
      {chargement && <Spin />}
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginBottom: 16 }} />}
      {succes && <Alert type="success" message={succes} showIcon style={{ marginBottom: 16 }} />}
      {sessions !== null && (
        <>
          <Table rowKey="id" dataSource={sessions} columns={colonnes} pagination={false} scroll={{ x: true }} />
          <Space wrap style={{ marginTop: 16 }}>
            <Select
              style={{ minWidth: 220 }}
              placeholder="Session non clôturée"
              options={sessionsOuvertes.map((s) => ({ value: s.id, label: s.titre }))}
              onChange={(valeur) => setSessionId(valeur)}
            />
            <Select
              style={{ minWidth: 220 }}
              placeholder="Étudiant"
              showSearch
              optionFilterProp="label"
              options={etudiants.map((e) => ({ value: e.id, label: e.nom }))}
              onChange={(valeur) => setEtudiantId(valeur)}
            />
            <Button type="primary" disabled={sessionId === null || etudiantId === null} onClick={ajouterPresence}>
              Ajouter la présence
            </Button>
          </Space>
        </>
      )}
    </Card>
  );
}
