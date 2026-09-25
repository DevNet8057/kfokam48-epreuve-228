import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Button, Card, Select, Space, Spin, Typography } from "antd";
import { api, Etudiant, Promotion } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";
import { Profil, useIdentite } from "../../identite/IdentiteContext";

const { Title, Paragraph } = Typography;

/**
 * K48-25 : choix du profil, puis promotion + nom pour étudiant/relecteur (Q1 — pas de mot de passe).
 * Le formateur accède directement à /formateur (H13, risque accepté).
 */
export function ChoixIdentite() {
  const navigate = useNavigate();
  const { seConnecter } = useIdentite();

  const [profil, setProfil] = useState<Profil | null>(null);
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [promotionId, setPromotionId] = useState<number | null>(null);
  const [etudiants, setEtudiants] = useState<Etudiant[]>([]);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  function choisirFormateur() {
    seConnecter({ profil: "FORMATEUR" });
    navigate("/formateur");
  }

  async function choisirEtudiantOuRelecteur(profilChoisi: Profil) {
    setProfil(profilChoisi);
    setErreur(null);
    setChargement(true);
    try {
      setPromotions(await api.listerPromotions());
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de charger les promotions.");
    } finally {
      setChargement(false);
    }
  }

  useEffect(() => {
    if (promotionId === null) {
      setEtudiants([]);
      return;
    }
    let annule = false;
    setChargement(true);
    setErreur(null);
    api
      .listerEtudiantsDeLaPromotion(promotionId)
      .then((reponse) => {
        if (!annule) setEtudiants(reponse);
      })
      .catch((cause) => {
        if (!annule) setErreur(cause instanceof ErreurApi ? cause.message : "Cette promotion n'existe pas.");
      })
      .finally(() => {
        if (!annule) setChargement(false);
      });
    return () => {
      annule = true;
    };
  }, [promotionId]);

  function choisirEtudiant(etudiant: Etudiant) {
    if (profil === null) return;
    seConnecter({ profil, etudiantId: etudiant.id, nom: etudiant.nom });
    navigate(profil === "ETUDIANT" ? "/etudiant" : "/relecteur");
  }

  return (
    <Card style={{ maxWidth: 480, margin: "2rem auto" }}>
      <Title level={3}>Qui êtes-vous ?</Title>

      {profil === null && (
        <Space direction="vertical" style={{ width: "100%" }}>
          <Button block onClick={choisirFormateur}>
            Formateur
          </Button>
          <Button block onClick={() => choisirEtudiantOuRelecteur("ETUDIANT")}>
            Étudiant
          </Button>
          <Button block onClick={() => choisirEtudiantOuRelecteur("RELECTEUR")}>
            Relecteur
          </Button>
        </Space>
      )}

      {profil !== null && (
        <Space direction="vertical" style={{ width: "100%" }}>
          <Paragraph type="secondary">
            {profil === "ETUDIANT" ? "Étudiant" : "Relecteur"} — choisissez votre promotion puis votre nom.
          </Paragraph>

          <Select
            style={{ width: "100%" }}
            placeholder="Promotion"
            showSearch
            optionFilterProp="label"
            options={promotions.map((p) => ({ value: p.id, label: p.nom }))}
            onChange={(valeur) => setPromotionId(valeur)}
            loading={chargement && promotions.length === 0}
          />

          {promotionId !== null && (
            <Select
              style={{ width: "100%" }}
              placeholder="Votre nom"
              showSearch
              optionFilterProp="label"
              options={etudiants.map((e) => ({ value: e.id, label: e.nom }))}
              onChange={(valeur) => {
                const etudiant = etudiants.find((e) => e.id === valeur);
                if (etudiant) choisirEtudiant(etudiant);
              }}
            />
          )}

          <Button onClick={() => setProfil(null)}>Retour</Button>
        </Space>
      )}

      {chargement && <Spin style={{ marginTop: 16 }} />}
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginTop: 16 }} />}
    </Card>
  );
}
